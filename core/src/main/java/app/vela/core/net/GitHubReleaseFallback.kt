package app.vela.core.net

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resilient multi-path downloader for GitHub Release assets
 * (`https://github.com/<owner>/<repo>/releases/download/<tag>/<asset>`).
 *
 * When GitHub's web release redirector returns 5xx (such as `504 Gateway Time-out`) or times out,
 * this interceptor automatically resolves the asset ID via the GitHub Releases REST API
 * (`https://api.github.com/repos/<owner>/<repo>/releases/tags/<tag>`) and streams the binary
 * directly from `https://api.github.com/repos/<owner>/<repo>/releases/assets/<id>` with
 * `Accept: application/octet-stream` (which redirects directly to `release-assets.githubusercontent.com`).
 */
object GitHubReleaseFallback {
    private val RELEASE_REGEX = Regex("""^https://github\.com/([^/]+)/([^/]+)/releases/download/([^/]+)/([^?]+).*$""")

    // Cache of "<owner>/<repo>/<tag>" -> Map<assetName, assetId>
    private val tagAssetCache = ConcurrentHashMap<String, Map<String, Long>>()

    // Timestamp until which direct github.com/releases/download is known to be failing (5xx/timeout),
    // so subsequent requests go straight to the API without waiting for a timeout first.
    @Volatile private var directDegradedUntilMs: Long = 0L

    private val apiHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val interceptor = Interceptor { chain ->
        val req = chain.request()
        val urlStr = req.url.toString()
        val match = RELEASE_REGEX.matchEntire(urlStr) ?: return@Interceptor chain.proceed(req)

        val owner = match.groupValues[1]
        val repo = match.groupValues[2]
        val tag = match.groupValues[3]
        val assetName = match.groupValues[4]

        val now = System.currentTimeMillis()
        if (now < directDegradedUntilMs) {
            resolveViaApi(req, owner, repo, tag, assetName)?.let { return@Interceptor it }
        }

        val initialResp: Response? = try {
            chain.withConnectTimeout(6, TimeUnit.SECONDS)
                .withReadTimeout(12, TimeUnit.SECONDS)
                .proceed(req)
        } catch (e: IOException) {
            directDegradedUntilMs = System.currentTimeMillis() + 10 * 60_000L
            resolveViaApi(req, owner, repo, tag, assetName) ?: throw e
        }

        if (initialResp != null && initialResp.code >= 500) {
            directDegradedUntilMs = System.currentTimeMillis() + 10 * 60_000L
            val fallback = resolveViaApi(req, owner, repo, tag, assetName)
            if (fallback != null) {
                runCatching { initialResp.close() }
                return@Interceptor fallback
            }
        }

        initialResp!!
    }

    private fun resolveViaApi(
        originalReq: Request,
        owner: String,
        repo: String,
        tag: String,
        assetName: String,
    ): Response? = runCatching {
        val key = "$owner/$repo/$tag"
        val map = tagAssetCache[key] ?: fetchTagAssets(owner, repo, tag)?.also {
            if (it.isNotEmpty()) tagAssetCache[key] = it
        } ?: return null

        val assetId = map[assetName] ?: return null
        val builder = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/assets/$assetId")
            .header("Accept", "application/octet-stream")
            .header("User-Agent", "Vela-Maps-Android")
        originalReq.header("Range")?.let { builder.header("Range", it) }

        val resp = apiHttp.newCall(builder.build()).execute()
        if (resp.isSuccessful || resp.code == 206) resp else {
            resp.close()
            null
        }
    }.getOrNull()

    private fun fetchTagAssets(owner: String, repo: String, tag: String): Map<String, Long>? = runCatching {
        val req = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/releases/tags/$tag")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Vela-Maps-Android")
            .build()
        apiHttp.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val assets = JSONObject(body).optJSONArray("assets") ?: return null
            val out = HashMap<String, Long>(assets.length())
            for (i in 0 until assets.length()) {
                val obj = assets.getJSONObject(i)
                val name = obj.optString("name", "")
                val id = obj.optLong("id", -1L)
                if (name.isNotEmpty() && id > 0L) {
                    out[name] = id
                }
            }
            out
        }
    }.getOrNull()
}
