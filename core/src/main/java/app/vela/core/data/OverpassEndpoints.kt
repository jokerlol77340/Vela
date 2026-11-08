package app.vela.core.data

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.net.URLEncoder

/**
 * Shared endpoint list + failover runner for every keyless **Overpass** (OpenStreetMap query API) fetch in
 * Vela: ALPR/Flock cameras ([OverpassAlprCameras]), traffic controls ([OverpassTrafficSignals]) and the
 * offline OSM POI/address index ([OverpassPois]).
 *
 * Fast and reliable community mirrors (ovp.dresel.systems, maprva.org) are preferred first to guarantee
 * sub-second response times without timeouts or IP-level blocking.
 */
object OverpassEndpoints {
    /** Overpass mirrors are community infrastructure and their policies want a contactable client,
     *  so this is the honest identifier, never the scrape's browser UA. Single-sourced from
     *  [app.vela.core.VelaConfig.VELA_UA]. */
    private val USER_AGENT: String get() = app.vela.core.VelaConfig.VELA_UA

    /** Primary fast mirrors first, then fallbacks. Order is preference; each is tried until one answers 2xx. */
    val ENDPOINTS = listOf(
        "https://ovp.dresel.systems/api/interpreter",
        "https://overpass.maprva.org/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
    )

    /**
     * Run [query] against each endpoint in order and hand the FIRST successful (2xx) response body to
     * [onBody], returning its result. Uses standard POST form body first, with GET fallback.
     */
    fun <T> run(http: OkHttpClient, query: String, onBody: (ResponseBody) -> T): T? {
        for (ep in ENDPOINTS) {
            val formBody = FormBody.Builder().add("data", query).build()
            val postReq = Request.Builder()
                .url(ep)
                .post(formBody)
                .header("User-Agent", USER_AGENT)
                .build()
            try {
                http.newCall(postReq).execute().use { resp ->
                    val body = resp.body
                    if (resp.isSuccessful && body != null) return onBody(body)
                }
            } catch (e: Exception) {
                // Network / timeout / error on POST: try GET fallback before advancing to next endpoint
                try {
                    val url = "$ep?data=" + URLEncoder.encode(query, "UTF-8")
                    val getReq = Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .build()
                    http.newCall(getReq).execute().use { resp ->
                        val body = resp.body
                        if (resp.isSuccessful && body != null) return onBody(body)
                    }
                } catch (e2: Exception) {
                    // fall through to next mirror
                }
            }
        }
        return null
    }
}
