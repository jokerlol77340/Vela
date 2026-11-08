package app.vela.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import app.vela.core.data.OverpassAlprCameras
import app.vela.core.model.LatLng
import app.vela.core.model.distanceTo
import app.vela.ui.MemoryPressure
import app.vela.ui.WazeAlert
import app.vela.ui.WazeAlertKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * One-shot fetcher for Waze LiveMap community alerts (`POLICE`, `HAZARD`, `ACCIDENT`) along a
 * calculated route corridor.
 *
 * Called **once** after the best/safest route is chosen. Strictly deduplicates against all known
 * fixed speed cameras (`BundledSpeedCameras`, `RoadFeatures`, `OverpassSpeedCameras`) and
 * ALPR/surveillance cameras (`FlockCameras`, `OverpassAlprCameras`) so radars are never duplicated.
 */
object WazeLiveFetcher {
    private const val LIVE_MAP_URL = "https://www.waze.com/live-map"
    private const val RECAPTCHA_SITE_KEY = "6Lf4WdUqAAAAAEUYUvzyLYIkO3PoFAqi8ZHGiDLW"
    private const val ROUTE_CORRIDOR_M = 85.0
    private const val EXISTING_CAMERA_DEDUPE_M = 130.0
    private const val SELF_DEDUPE_M = 120.0
    private const val IDLE_REAP_MS = 90_000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private var pageReady: CompletableDeferred<Boolean>? = null
    private val reapRunnable = Runnable { destroyWebView() }

    init {
        MemoryPressure.register { level ->
            if (MemoryPressure.isSevere(level)) {
                mainHandler.post { destroyWebView() }
            }
        }
    }

    /**
     * Fetch live Waze alerts along [polyline] once, removing any radar/police-radar alert that
     * sits within [EXISTING_CAMERA_DEDUPE_M] of [existingSpeedCams] or [existingFlockCams].
     */
    suspend fun fetchAlongRoute(
        context: Context,
        http: OkHttpClient,
        polyline: List<LatLng>,
        existingSpeedCams: List<LatLng>,
        existingFlockCams: List<LatLng>,
    ): List<WazeAlert> = withContext(Dispatchers.IO) {
        if (polyline.size < 2) return@withContext emptyList()
        val tiles = OverpassAlprCameras.routeTiles(
            polyline = polyline,
            padDeg = 0.008,
            maxSpanDeg = 0.35,
            maxTiles = 4,
        )
        if (tiles.isEmpty()) return@withContext emptyList()

        val urls = tiles.map { t ->
            val south = t[0]; val west = t[1]; val north = t[2]; val east = t[3]
            val midLat = (south + north) * 0.5
            val midLng = (west + east) * 0.5
            val env = when {
                midLng < -30.0 -> "na"
                midLat in 29.0..34.0 && midLng in 34.0..36.0 -> "il"
                else -> "row"
            }
            String.format(
                Locale.US,
                "https://www.waze.com/live-map/api/georss?top=%.5f&bottom=%.5f&left=%.5f&right=%.5f&env=%s&types=alerts",
                north, south, west, east, env,
            )
        }

        val rawBodies = ArrayList<String>(urls.size)
        var needWebViewFallback = false
        val directClient = http.newBuilder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()

        for (url in urls) {
            val body = runCatching {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36")
                    .header("Referer", LIVE_MAP_URL)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .build()
                directClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }
            }.getOrNull()
            if (body != null && body.contains("\"alerts\"")) {
                rawBodies.add(body)
            } else {
                needWebViewFallback = true
                break
            }
        }

        if (needWebViewFallback && rawBodies.isEmpty()) {
            val fb = fetchViaWebView(context.applicationContext, urls)
            rawBodies.addAll(fb)
        }

        if (rawBodies.isEmpty()) return@withContext emptyList()
        parseAndDedupe(rawBodies, polyline, existingSpeedCams, existingFlockCams)
    }

    private fun parseAndDedupe(
        bodies: List<String>,
        polyline: List<LatLng>,
        existingSpeedCams: List<LatLng>,
        existingFlockCams: List<LatLng>,
    ): List<WazeAlert> {
        val out = ArrayList<WazeAlert>()
        val seenIds = HashSet<String>()

        for (body in bodies) {
            val alertsArr = runCatching {
                JSONObject(body).optJSONArray("alerts")
            }.getOrNull() ?: continue

            for (i in 0 until alertsArr.length()) {
                val obj = alertsArr.optJSONObject(i) ?: continue
                val uuid = obj.optString("uuid", obj.optString("id", ""))
                if (uuid.isNotEmpty() && !seenIds.add(uuid)) continue

                val type = obj.optString("type", "").uppercase(Locale.US)
                val subtype = obj.optString("subtype", "").uppercase(Locale.US)

                // Pure fixed cameras ("CAMERA") are already in our multi-source radar database.
                if (type == "CAMERA") continue

                val kind = when (type) {
                    "POLICE" -> WazeAlertKind.POLICE
                    "HAZARD", "ROAD_CLOSED" -> WazeAlertKind.HAZARD
                    "ACCIDENT" -> WazeAlertKind.ACCIDENT
                    else -> continue
                }

                val locObj = obj.optJSONObject("location") ?: continue
                val lat = locObj.optDouble("y", Double.NaN)
                val lng = locObj.optDouble("x", Double.NaN)
                if (lat.isNaN() || lng.isNaN()) continue
                val loc = LatLng(lat, lng)

                // Keep only alerts right along the active route corridor.
                if (!OverpassAlprCameras.nearPolyline(loc, polyline, ROUTE_CORRIDOR_M)) continue

                // Strict deduplication against existing speed cameras and ALPR/surveillance cameras:
                // if a Waze police/mobile-radar report sits near an already-cataloged fixed radar
                // or camera, drop it so the driver never gets duplicate icons or duplicate warnings.
                if (kind == WazeAlertKind.POLICE || subtype.contains("CAMERA") || subtype.contains("RADAR")) {
                    val nearKnownSpeedCam = existingSpeedCams.any { it.distanceTo(loc) <= EXISTING_CAMERA_DEDUPE_M }
                    if (nearKnownSpeedCam) continue
                    val nearKnownFlockCam = existingFlockCams.any { it.distanceTo(loc) <= EXISTING_CAMERA_DEDUPE_M }
                    if (nearKnownFlockCam) continue
                }

                // Deduplicate Waze alerts against each other (cluster within 120 m for same kind).
                if (out.any { it.kind == kind && it.loc.distanceTo(loc) <= SELF_DEDUPE_M }) continue

                val street = obj.optString("street", "")
                out.add(
                    WazeAlert(
                        id = uuid.ifEmpty { "${kind.name}:${(lat * 1e5).toLong()},${(lng * 1e5).toLong()}" },
                        loc = loc,
                        kind = kind,
                        subtype = subtype,
                        street = street,
                    ),
                )
            }
        }
        return out
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun ensureWebViewReady(appContext: Context): WebView? = withContext(Dispatchers.Main) {
        mainHandler.removeCallbacks(reapRunnable)
        mainHandler.postDelayed(reapRunnable, IDLE_REAP_MS)

        val existing = webView
        val existingReady = pageReady
        if (existing != null && existingReady != null && existingReady.isCompleted) {
            return@withContext existing
        }

        val ready = CompletableDeferred<Boolean>()
        pageReady = ready
        val wv = existing ?: runCatching {
            WebView(appContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.blockNetworkImage = true
                settings.loadsImagesAutomatically = false
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val u = request?.url?.toString()?.lowercase(Locale.US).orEmpty()
                        // Block heavy map tiles, fonts, images, and analytics so only Waze JS + reCAPTCHA load.
                        if (u.endsWith(".png") || u.endsWith(".jpg") || u.endsWith(".webp") ||
                            u.endsWith(".woff2") || u.endsWith(".ttf") || u.endsWith(".svg") ||
                            u.contains("/tiles/") || u.contains(" doubleclick.") || u.contains("google-analytics.")
                        ) {
                            return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (!ready.isCompleted) ready.complete(true)
                    }
                }
            }.also { webView = it }
        }.getOrNull() ?: return@withContext null

        wv.loadUrl(LIVE_MAP_URL)
        val ok = withTimeoutOrNull(10_000L) { ready.await() } ?: false
        if (ok) wv else null
    }

    private suspend fun fetchViaWebView(appContext: Context, urls: List<String>): List<String> {
        val wv = ensureWebViewReady(appContext) ?: return emptyList()
        val urlsJson = JSONArray(urls).toString()
        val js = """
            (function() {
                var urls = $urlsJson;
                function runFetch(token) {
                    var headers = { 'Accept': 'application/json, text/plain, */*', 'X-Requested-With': 'XMLHttpRequest' };
                    if (token) headers['X-Recaptcha-Token'] = token;
                    return Promise.all(urls.map(function(u) {
                        return fetch(u, { headers: headers, credentials: 'include' })
                            .then(function(r) { return r.ok ? r.text() : ''; })
                            .catch(function() { return ''; });
                    }));
                }
                var p;
                if (window.grecaptcha && window.grecaptcha.enterprise && window.grecaptcha.enterprise.execute) {
                    p = new Promise(function(resolve) {
                        window.grecaptcha.enterprise.ready(function() {
                            window.grecaptcha.enterprise.execute('$RECAPTCHA_SITE_KEY', { action: 'api' })
                                .then(resolve)
                                .catch(function() { resolve(''); });
                        });
                    }).then(runFetch);
                } else {
                    p = runFetch('');
                }
                p.then(function(results) {
                    window._velaWazeResult = JSON.stringify(results);
                }).catch(function() {
                    window._velaWazeResult = '[]';
                });
                return 'started';
            })();
        """.trimIndent()

        return withContext(Dispatchers.Main) {
            mainHandler.removeCallbacks(reapRunnable)
            mainHandler.postDelayed(reapRunnable, IDLE_REAP_MS)
            wv.evaluateJavascript("window._velaWazeResult = null; $js", null)
            var resultJson: String? = null
            for (attempt in 0 until 24) {
                kotlinx.coroutines.delay(350)
                val poll = evalJs(wv, "window._velaWazeResult")
                if (!poll.isNullOrBlank() && poll != "null") {
                    resultJson = poll
                    break
                }
            }
            if (resultJson.isNullOrBlank()) return@withContext emptyList()
            runCatching {
                val decoded = if (resultJson.startsWith("\"")) {
                    org.json.JSONTokener(resultJson).nextValue().toString()
                } else resultJson
                val arr = JSONArray(decoded)
                val list = ArrayList<String>(arr.length())
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i, "")
                    if (s.contains("\"alerts\"")) list.add(s)
                }
                list
            }.getOrDefault(emptyList())
        }
    }

    private suspend fun evalJs(wv: WebView, script: String): String? =
        withTimeoutOrNull(2_000L) {
            suspendCancellableCoroutine { cont ->
                wv.evaluateJavascript(script) { value ->
                    if (cont.isActive) cont.resume(value)
                }
            }
        }

    private fun destroyWebView() {
        mainHandler.removeCallbacks(reapRunnable)
        pageReady = null
        webView?.let { wv ->
            runCatching {
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.destroy()
            }
        }
        webView = null
    }
}
