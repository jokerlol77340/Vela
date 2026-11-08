package app.vela.data

import android.content.Context
import app.vela.core.data.SpeedCamera
import app.vela.core.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-source bundled + official government speed camera store with spatial deduplication.
 *
 * Combines:
 * 1. Bundled `assets/speed_cameras.bin` (OSM speed cameras for France + neighbouring countries
 *    merged and deduplicated with the official French government `data.gouv.fr` radar dataset).
 * 2. Periodic background refresh from `data.gouv.fr` official radar CSV so newly installed
 *    radars are picked up automatically.
 * 3. Spatial deduplication helper [mergeAndDedupe] used across map viewport rendering, active
 *    navigation radar alerts, and route camera avoidance to combine `BundledSpeedCameras`,
 *    `RoadFeatures`, and live `OverpassSpeedCameras` without duplicates and without single-source
 *    failure points.
 */
object BundledSpeedCameras {
    private const val BUNDLED_ASSET = "speed_cameras.bin"
    private const val OFFICIAL_FR_CSV_URL =
        "https://static.data.gouv.fr/resources/radars-automatiques/20181025-141231/radars.csv"
    private const val REFRESH_TTL_MS = 7L * 24 * 60 * 60 * 1000 // 7 days
    private const val CELL = 0.1
    private const val DEDUPE_RADIUS_M = 45.0

    private val mutex = Mutex()

    @Volatile private var loaded = false
    private var lat = DoubleArray(0)
    private var lng = DoubleArray(0)
    private val grid = HashMap<Long, IntArray>()

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val isLoaded: Boolean get() = loaded
    val size: Int get() = lat.size

    private fun key(row: Long, col: Long): Long = (row shl 32) xor (col and 0xffffffffL)
    private fun rowOf(v: Double): Long = floor(v / CELL).toLong()

    private fun cacheDir(context: Context) = File(context.filesDir, "speed_cameras").apply { mkdirs() }
    private fun extraCacheFile(context: Context) = File(cacheDir(context), "official_fr_extra.bin")

    suspend fun ensureLoaded(context: Context) {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            withContext(Dispatchers.IO) {
                val pts = ArrayList<SpeedCamera>(75_000)
                runCatching {
                    context.assets.open(BUNDLED_ASSET).use { raw ->
                        pts.addAll(parseStream(raw))
                    }
                }
                val extra = extraCacheFile(context)
                if (extra.exists()) {
                    runCatching {
                        extra.inputStream().use { raw ->
                            pts.addAll(parseStream(raw))
                        }
                    }
                }
                if (pts.isNotEmpty()) {
                    publish(mergeAndDedupe(pts, radiusM = DEDUPE_RADIUS_M))
                }
            }
        }
    }

    /**
     * Persist newly discovered speed cameras from live queries onto disk,
     * ensuring they are stored in the local offline database immediately.
     */
    suspend fun rememberNewCameras(context: Context, newCameras: List<SpeedCamera>) = withContext(Dispatchers.IO) {
        if (newCameras.isEmpty()) return@withContext
        mutex.withLock {
            val current = toList()
            val deduped = mergeAndDedupe(current, newCameras, radiusM = DEDUPE_RADIUS_M)
            if (deduped.size > current.size) {
                publish(deduped)
                runCatching {
                    val extra = extraCacheFile(context)
                    val tmp = File(cacheDir(context), "official_fr_extra.bin.tmp")
                    GZIPOutputStream(tmp.outputStream()).bufferedWriter(Charsets.UTF_8).use { w ->
                        for (c in deduped) {
                            w.write("${c.loc.lat}\t${c.loc.lng}\tC\n")
                        }
                    }
                    if (!tmp.renameTo(extra)) {
                        tmp.copyTo(extra, overwrite = true)
                        tmp.delete()
                    }
                }
            }
        }
    }

    /**
     * Best-effort periodic refresh of the official French government radar CSV (`data.gouv.fr`),
     * merging any new entries with the bundled dataset without duplicates.
     */
    suspend fun refreshOfficial(context: Context) = withContext(Dispatchers.IO) {
        ensureLoaded(context)
        val extra = extraCacheFile(context)
        val now = System.currentTimeMillis()
        if (extra.exists() && now - extra.lastModified() < REFRESH_TTL_MS) return@withContext
        runCatching {
            val csvText = http.newCall(Request.Builder().url(OFFICIAL_FR_CSV_URL).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext
                resp.body?.string().orEmpty()
            }
            if (csvText.isBlank()) return@withContext
            val parsed = parseOfficialCsv(csvText)
            if (parsed.isEmpty()) return@withContext
            val tmp = File(cacheDir(context), "official_fr_extra.bin.tmp")
            GZIPOutputStream(tmp.outputStream()).bufferedWriter(Charsets.UTF_8).use { w ->
                for (c in parsed) {
                    w.write("${c.loc.lat}\t${c.loc.lng}\tC\n")
                }
            }
            if (!tmp.renameTo(extra)) {
                tmp.copyTo(extra, overwrite = true)
                tmp.delete()
            }
            mutex.withLock {
                val current = toList()
                publish(mergeAndDedupe(current, parsed, radiusM = DEDUPE_RADIUS_M))
            }
        }
    }

    private fun parseStream(raw: InputStream): List<SpeedCamera> {
        val out = ArrayList<SpeedCamera>(75_000)
        GZIPInputStream(raw).bufferedReader(Charsets.UTF_8).useLines { lines ->
            for (line in lines) {
                val t1 = line.indexOf('\t'); if (t1 <= 0) continue
                val t2 = line.indexOf('\t', t1 + 1)
                val la = line.substring(0, t1).toDoubleOrNull() ?: continue
                val lo = (if (t2 < 0) line.substring(t1 + 1) else line.substring(t1 + 1, t2)).toDoubleOrNull() ?: continue
                if (t2 >= 0) {
                    val t3 = line.indexOf('\t', t2 + 1)
                    val kind = if (t3 < 0) line.substring(t2 + 1) else line.substring(t2 + 1, t3)
                    if (kind.isNotEmpty() && kind[0] != 'C') continue
                }
                out.add(SpeedCamera(LatLng(la, lo)))
            }
        }
        return out
    }

    private fun parseOfficialCsv(csvText: String): List<SpeedCamera> {
        val lines = csvText.lineSequence().iterator()
        if (!lines.hasNext()) return emptyList()
        val header = lines.next().trimStart('\uFEFF').split(',')
        val latIdx = header.indexOfFirst { it.trim().equals("latitude", ignoreCase = true) }
        val lonIdx = header.indexOfFirst { it.trim().equals("longitude", ignoreCase = true) }
        if (latIdx < 0 || lonIdx < 0) return emptyList()
        val out = ArrayList<SpeedCamera>(3500)
        while (lines.hasNext()) {
            val cols = lines.next().split(',')
            if (cols.size <= max(latIdx, lonIdx)) continue
            val la = cols[latIdx].trim().toDoubleOrNull() ?: continue
            val lo = cols[lonIdx].trim().toDoubleOrNull() ?: continue
            if (la in -90.0..90.0 && lo in -180.0..180.0) {
                out.add(SpeedCamera(LatLng(la, lo)))
            }
        }
        return out
    }

    private fun toList(): List<SpeedCamera> {
        val la = lat; val lo = lng
        return List(la.size) { i -> SpeedCamera(LatLng(la[i], lo[i])) }
    }

    private fun publish(cameras: List<SpeedCamera>) {
        val n = cameras.size
        val la = DoubleArray(n)
        val lo = DoubleArray(n)
        val keys = LongArray(n)
        val counts = HashMap<Long, Int>()
        for (i in 0 until n) {
            val c = cameras[i]
            la[i] = c.loc.lat
            lo[i] = c.loc.lng
            val k = key(rowOf(la[i]), rowOf(lo[i]))
            keys[i] = k
            counts[k] = (counts[k] ?: 0) + 1
        }
        val g = HashMap<Long, IntArray>(counts.size * 2)
        for ((k, c) in counts) g[k] = IntArray(c)
        val fill = HashMap<Long, Int>(counts.size * 2)
        for (i in 0 until n) {
            val k = keys[i]
            val at = fill[k] ?: 0
            g[k]!![at] = i
            fill[k] = at + 1
        }
        lat = la
        lng = lo
        synchronized(grid) {
            grid.clear()
            grid.putAll(g)
        }
        loaded = true
    }

    /** Speed cameras inside the bounding box. */
    fun inBox(south: Double, west: Double, north: Double, east: Double): List<SpeedCamera> {
        if (!loaded) return emptyList()
        val la = lat; val lo = lng
        val out = ArrayList<SpeedCamera>()
        val r0 = rowOf(south); val r1 = rowOf(north)
        val c0 = rowOf(west); val c1 = rowOf(east)
        synchronized(grid) {
            var r = r0
            while (r <= r1) {
                var c = c0
                while (c <= c1) {
                    grid[key(r, c)]?.let { bucket ->
                        for (i in bucket) {
                            if (la[i] in south..north && lo[i] in west..east) {
                                out.add(SpeedCamera(LatLng(la[i], lo[i])))
                            }
                        }
                    }
                    c++
                }
                r++
            }
        }
        return out
    }

    /** Speed cameras within [nearMeters] of [polyline]. */
    fun along(polyline: List<LatLng>, nearMeters: Double = 150.0): List<SpeedCamera> {
        if (!loaded || polyline.size < 2) return emptyList()
        val degPad = nearMeters / 111_000.0 * 1.5
        var s = 90.0; var n = -90.0; var w = 180.0; var e = -180.0
        for (p in polyline) {
            if (p.lat < s) s = p.lat; if (p.lat > n) n = p.lat
            if (p.lng < w) w = p.lng; if (p.lng > e) e = p.lng
        }
        val candidates = inBox(s - degPad, w - degPad, n + degPad, e + degPad)
        if (candidates.isEmpty()) return emptyList()
        return candidates.filter { cam -> nearPolyline(cam.loc.lat, cam.loc.lng, polyline, nearMeters) }
    }

    private fun nearPolyline(lat: Double, lng: Double, poly: List<LatLng>, maxM: Double): Boolean {
        val deg = maxM / 111_000.0 * 1.5
        val cosLat = cos(Math.toRadians(lat))
        val mPerDegLat = 111_132.0
        val mPerDegLng = 111_320.0 * cosLat
        val r2 = maxM * maxM
        for (i in 0 until poly.size - 1) {
            val a = poly[i]; val b = poly[i + 1]
            if (lat < min(a.lat, b.lat) - deg || lat > max(a.lat, b.lat) + deg) continue
            if (lng < min(a.lng, b.lng) - deg || lng > max(a.lng, b.lng) + deg) continue
            val ax = (a.lng - lng) * mPerDegLng; val ay = (a.lat - lat) * mPerDegLat
            val bx = (b.lng - lng) * mPerDegLng; val by = (b.lat - lat) * mPerDegLat
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 > 0.0) (-(ax * dx + ay * dy) / len2).coerceIn(0.0, 1.0) else 0.0
            val px = ax + t * dx; val py = ay + t * dy
            if (px * px + py * py <= r2) return true
        }
        return false
    }

    /**
     * Combines multiple speed camera lists from different sources (`BundledSpeedCameras`,
     * `RoadFeatures`, `OverpassSpeedCameras`) and removes spatial duplicates within [radiusM].
     */
    fun mergeAndDedupe(vararg lists: List<SpeedCamera>, radiusM: Double = DEDUPE_RADIUS_M): List<SpeedCamera> {
        val total = lists.sumOf { it.size }
        if (total == 0) return emptyList()
        val cellDeg = 0.002 // ~220 m
        val r2 = radiusM * radiusM
        val out = ArrayList<SpeedCamera>(total)
        val spatial = HashMap<Long, MutableList<Int>>()

        fun cellKey(r: Long, c: Long): Long = (r shl 32) xor (c and 0xffffffffL)

        for (list in lists) {
            for (cam in list) {
                val la = cam.loc.lat
                val lo = cam.loc.lng
                val cr = floor(la / cellDeg).toLong()
                val cc = floor(lo / cellDeg).toLong()
                val cosLat = cos(Math.toRadians(la))
                val mPerDegLat = 111_132.0
                val mPerDegLng = 111_320.0 * cosLat
                var matchIdx = -1
                var dr = -1L
                while (dr <= 1L && matchIdx < 0) {
                    var dc = -1L
                    while (dc <= 1L) {
                        spatial[cellKey(cr + dr, cc + dc)]?.let { bucket ->
                            for (idx in bucket) {
                                val existing = out[idx]
                                val dy = (la - existing.loc.lat) * mPerDegLat
                                val dx = (lo - existing.loc.lng) * mPerDegLng
                                if (dx * dx + dy * dy <= r2) {
                                    matchIdx = idx
                                    break
                                }
                            }
                        }
                        if (matchIdx >= 0) break
                        dc++
                    }
                    dr++
                }
                if (matchIdx < 0) {
                    val newIdx = out.size
                    out.add(cam)
                    spatial.getOrPut(cellKey(cr, cc)) { ArrayList(2) }.add(newIdx)
                }
            }
        }
        return out
    }
}
