package app.vela.data

import android.content.Context
import app.vela.core.data.AlprCamera
import app.vela.core.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * The ALPR / Flock surveillance-camera dataset, queried ON-DEVICE.
 *
 * The cameras are the community DeFlock project's OpenStreetMap nodes (`surveillance:type=ALPR`). The whole
 * global set is tiny (~124k points), so instead of a live Overpass query per viewport (slow, and it 504s
 * under load) the map layer + route "passes N cameras" count read a compact baked file: gzipped TSV
 * `lat<TAB>lon<TAB>operator`, parsed once off the main thread into flat arrays + a 0.1 deg grid index.
 *
 * TWO tiers of that file, newest wins:
 *  - a **bundled** floor in `assets/flock_cameras.bin` (+ its version in `assets/flock_cameras_version.txt`),
 *    so a fresh install has cameras immediately and offline;
 *  - a **hosted** copy on the `flock-cameras` GitHub release, refreshed by CI (weekly cron), which
 *    [refresh] downloads to `filesDir/flock/cameras.bin` when the manifest version beats what's installed -
 *    so camera data updates WITHOUT shipping an app release (the user's ask 2026-07-13).
 *
 * `OverpassAlprCameras` stays as the fallback for the brief window before the file finishes loading.
 */
object FlockCameras {
    private const val BUNDLED = "flock_cameras.bin"
    private const val BUNDLED_VER = "flock_cameras_version.txt"
    private const val BUNDLED_SURV = "surveillance_cameras.bin"
    private const val CELL = 0.1 // grid cell size in degrees (~11 km) for the bucket index

    @Volatile private var loaded = false
    private var lat = DoubleArray(0)
    private var lng = DoubleArray(0)
    private var op = arrayOf<String>()
    // Camera FACING in degrees clockwise from north (the 2026-07-21 4th TSV column); NaN when the
    // node is untagged or the file predates the column - 3-column datasets keep decoding.
    private var dir = FloatArray(0)
    private var isAlprArr = BooleanArray(0)
    private val grid = HashMap<Long, MutableList<Int>>()

    val isLoaded: Boolean get() = loaded
    val size: Int get() = lat.size

    private fun key(row: Long, col: Long): Long = (row shl 32) xor (col and 0xffffffffL)
    private fun rowOf(v: Double): Long = Math.floor(v / CELL).toLong()
    /** The facing for camera [i] in AlprCamera's string form ("165"), empty when untagged. */
    private fun dirStr(i: Int): String {
        val d = dir.getOrElse(i) { Float.NaN }
        return if (d.isNaN()) "" else d.toInt().toString()
    }

    private fun dir(context: Context) = File(context.filesDir, "flock").apply { mkdirs() }
    private fun downloadedBin(context: Context) = File(dir(context), "cameras.bin")
    private fun downloadedVer(context: Context) = File(dir(context), "version.txt")
    private fun liveSurvBin(context: Context) = File(dir(context), "live_surveillance.bin")

    private fun bundledVersion(context: Context): Int =
        runCatching { context.assets.open(BUNDLED_VER).bufferedReader().use { it.readText() }.trim().toInt() }
            .getOrDefault(0)

    private fun downloadedVersion(context: Context): Int {
        val d = downloadedVer(context)
        if (!downloadedBin(context).exists() || !d.exists()) return -1
        return d.readText().trim().toIntOrNull() ?: -1
    }

    /** The newest version available locally: max of the downloaded copy and the bundled floor. */
    private fun installedVersion(context: Context): Int =
        maxOf(downloadedVersion(context), bundledVersion(context))

    /**
     * Parse both ALPR and surveillance datasets once, off the main thread. Safe to call repeatedly.
     */
    suspend fun ensureLoaded(context: Context) {
        if (loaded) return
        withContext(Dispatchers.IO) {
            if (loaded) return@withContext
            loadAll(context)
        }
    }

    private fun loadAll(context: Context) {
        val dl = downloadedBin(context)
        val useDownload = downloadedVersion(context) >= bundledVersion(context) && dl.exists()
        if (!useDownload && dl.exists()) {
            runCatching { dl.delete(); downloadedVer(context).delete() }
        }
        val alprStream = if (useDownload) runCatching { dl.inputStream() }.getOrNull()
            else runCatching { context.assets.open(BUNDLED) }.getOrNull()

        val survStream = runCatching { context.assets.open(BUNDLED_SURV) }.getOrNull()
        val liveSurv = liveSurvBin(context).takeIf { it.exists() }?.let { runCatching { it.inputStream() }.getOrNull() }

        val las = ArrayList<Double>(280_000)
        val los = ArrayList<Double>(280_000)
        val ops = ArrayList<String>(280_000)
        val dirs = ArrayList<Float>(280_000)
        val alprs = ArrayList<Boolean>(280_000)
        val intern = HashMap<String, String>()

        // 1. Load ALPR cameras
        if (alprStream != null) {
            runCatching {
                parseInto(alprStream, true, las, los, ops, dirs, alprs, intern)
            }
        }
        val alprCount = las.size

        // Build spatial index of ALPR points to strictly avoid any duplicate with surveillance cameras
        val alprSpatial = HashMap<Long, MutableList<Int>>()
        val cellDeg = 0.002
        fun cKey(r: Long, c: Long): Long = (r shl 32) xor (c and 0xffffffffL)
        for (i in 0 until alprCount) {
            val cr = Math.floor(las[i] / cellDeg).toLong()
            val cc = Math.floor(los[i] / cellDeg).toLong()
            alprSpatial.getOrPut(cKey(cr, cc)) { ArrayList(2) }.add(i)
        }

        fun isNearAlpr(la: Double, lo: Double, radiusM: Double = 30.0): Boolean {
            val cr = Math.floor(la / cellDeg).toLong()
            val cc = Math.floor(lo / cellDeg).toLong()
            val cosLat = Math.cos(Math.toRadians(la))
            val mLat = 111_132.0
            val mLng = 111_320.0 * cosLat
            val r2 = radiusM * radiusM
            var dr = -1L
            while (dr <= 1L) {
                var dc = -1L
                while (dc <= 1L) {
                    alprSpatial[cKey(cr + dr, cc + dc)]?.let { bucket ->
                        for (idx in bucket) {
                            val dy = (la - las[idx]) * mLat
                            val dx = (lo - los[idx]) * mLng
                            if (dx * dx + dy * dy <= r2) return true
                        }
                    }
                    dc++
                }
                dr++
            }
            return false
        }

        // 2. Load bundled surveillance cameras (with ALPR duplicate filter)
        if (survStream != null) {
            runCatching {
                parseInto(survStream, false, las, los, ops, dirs, alprs, intern, ::isNearAlpr)
            }
        }

        // 3. Load extra live surveillance cameras cached on disk (with ALPR duplicate filter)
        if (liveSurv != null) {
            runCatching {
                parseInto(liveSurv, false, las, los, ops, dirs, alprs, intern, ::isNearAlpr)
            }
        }

        val g = HashMap<Long, MutableList<Int>>()
        for (i in las.indices) g.getOrPut(key(rowOf(las[i]), rowOf(los[i]))) { ArrayList() }.add(i)

        lat = las.toDoubleArray()
        lng = los.toDoubleArray()
        op = ops.toTypedArray()
        dir = dirs.toFloatArray()
        val ab = BooleanArray(alprs.size)
        for (i in alprs.indices) ab[i] = alprs[i]
        isAlprArr = ab
        grid.clear()
        grid.putAll(g)
        loaded = true
    }

    private fun parseInto(
        raw: InputStream,
        isAlprDataset: Boolean,
        las: ArrayList<Double>,
        los: ArrayList<Double>,
        ops: ArrayList<String>,
        dirs: ArrayList<Float>,
        alprs: ArrayList<Boolean>,
        intern: HashMap<String, String>,
        alprFilter: ((Double, Double) -> Boolean)? = null,
    ) {
        raw.use { r ->
            GZIPInputStream(r).bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val t1 = line.indexOf('\t'); if (t1 <= 0) continue
                    val t2 = line.indexOf('\t', t1 + 1); if (t2 < 0) continue
                    val la = line.substring(0, t1).toDoubleOrNull() ?: continue
                    val lo = line.substring(t1 + 1, t2).toDoubleOrNull() ?: continue
                    if (alprFilter != null && alprFilter(la, lo)) continue
                    val t3 = line.indexOf('\t', t2 + 1)
                    val o: String
                    val dg: Float
                    var itemIsAlpr = isAlprDataset
                    if (t3 < 0) {
                        o = line.substring(t2 + 1)
                        dg = Float.NaN
                    } else {
                        o = line.substring(t2 + 1, t3)
                        val t4 = line.indexOf('\t', t3 + 1)
                        if (t4 < 0) {
                            dg = line.substring(t3 + 1).toFloatOrNull() ?: Float.NaN
                        } else {
                            dg = line.substring(t3 + 1, t4).toFloatOrNull() ?: Float.NaN
                            val explicitAlpr = line.substring(t4 + 1).toBooleanStrictOrNull()
                            if (explicitAlpr != null) itemIsAlpr = explicitAlpr
                        }
                    }
                    las.add(la)
                    los.add(lo)
                    ops.add(intern.getOrPut(o) { o })
                    dirs.add(dg)
                    alprs.add(itemIsAlpr)
                }
            }
        }
    }

    /**
     * Persist newly discovered surveillance cameras from live queries onto disk,
     * strictly excluding any ALPR camera to prevent duplicates.
     */
    suspend fun rememberNewCameras(context: Context, newCameras: List<AlprCamera>) = withContext(Dispatchers.IO) {
        val nonAlprOnly = newCameras.filter { !it.isAlpr }
        if (nonAlprOnly.isEmpty()) return@withContext
        val liveFile = liveSurvBin(context)
        val tmp = File(dir(context), "live_surveillance.bin.tmp")
        val existing = if (liveFile.exists()) {
            runCatching {
                val list = ArrayList<AlprCamera>()
                liveFile.inputStream().use { stream ->
                    GZIPInputStream(stream).bufferedReader(Charsets.UTF_8).useLines { lines ->
                        for (l in lines) {
                            val parts = l.split('\t')
                            if (parts.size >= 2) {
                                val la = parts[0].toDoubleOrNull() ?: continue
                                val lo = parts[1].toDoubleOrNull() ?: continue
                                val o = parts.getOrNull(2).orEmpty()
                                val d = parts.getOrNull(3).orEmpty()
                                list.add(AlprCamera(LatLng(la, lo), o, d, isAlpr = false))
                            }
                        }
                    }
                }
                list
            }.getOrDefault(emptyList())
        } else emptyList()

        val deduped = mergeAndDedupe(existing, nonAlprOnly, radiusM = 30.0)
        if (deduped.size > existing.size) {
            runCatching {
                java.util.zip.GZIPOutputStream(tmp.outputStream()).bufferedWriter(Charsets.UTF_8).use { w ->
                    for (c in deduped) {
                        w.write("${c.loc.lat}\t${c.loc.lng}\t${c.operator}\t${c.direction}\tfalse\n")
                    }
                }
                if (!tmp.renameTo(liveFile)) {
                    tmp.copyTo(liveFile, overwrite = true)
                    tmp.delete()
                }
            }
        }
    }

    private val downloadHttp: OkHttpClient by lazy {
        OkHttpClient.Builder().callTimeout(0, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).addInterceptor(app.vela.core.net.GitHubReleaseFallback.interceptor).build()
    }

    /**
     * Check the hosted manifest and, if it carries a newer version than what's on disk, download the fresh
     * dataset and reload from it. Best-effort: any network/parse failure keeps the current data. Called once
     * per launch after [ensureLoaded], so camera data can update without an app release.
     */
    suspend fun refresh(context: Context, manifestUrl: String) = withContext(Dispatchers.IO) {
        runCatching {
            val body = downloadHttp.newCall(Request.Builder().url(manifestUrl).build()).execute()
                .use { if (!it.isSuccessful) return@withContext; it.body?.string().orEmpty() }
            // org.json, not kotlinx.serialization: the :app module stays off kotlinx (a deliberate boundary).
            val obj = org.json.JSONObject(body)
            val version = obj.optInt("version", -1); if (version < 0) return@withContext
            val url = obj.optString("url", ""); if (url.isBlank()) return@withContext
            if (version <= installedVersion(context)) return@withContext
            // Download to a temp file, validate the gzip magic, then atomically swap into place.
            val tmp = File(dir(context), "cameras.bin.tmp")
            downloadHttp.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext
                val bytes = resp.body?.bytes() ?: return@withContext
                if (bytes.size < 2 || bytes[0] != 0x1f.toByte() || bytes[1] != 0x8b.toByte()) return@withContext // not gzip
                tmp.writeBytes(bytes)
            }
            val target = downloadedBin(context)
            if (!tmp.renameTo(target)) { tmp.copyTo(target, overwrite = true); tmp.delete() }
            downloadedVer(context).writeText(version.toString())
            loadAll(context) // hot-swap the in-memory set to the fresh data
        }
        Unit
    }

    /** Cameras inside the bbox, for DRAWING. Empty if not loaded yet (caller falls back to Overpass). */
    fun inBox(south: Double, west: Double, north: Double, east: Double): List<AlprCamera> {
        if (!loaded) return emptyList()
        val out = ArrayList<AlprCamera>()
        val r0 = rowOf(south); val r1 = rowOf(north)
        val c0 = rowOf(west); val c1 = rowOf(east)
        var r = r0
        while (r <= r1) {
            var c = c0
            while (c <= c1) {
                grid[key(r, c)]?.let { bucket ->
                    for (i in bucket) {
                        if (lat[i] in south..north && lng[i] in west..east) {
                            out.add(AlprCamera(LatLng(lat[i], lng[i]), op[i], dirStr(i), isAlpr = isAlprArr.getOrElse(i) { true }))
                        }
                    }
                }
                c++
            }
            r++
        }
        return out
    }

    /**
     * Combines multiple ALPR / surveillance camera lists (`FlockCameras` + `OverpassAlprCameras`)
     * and removes spatial duplicates within [radiusM] (default 35 m), preserving richer metadata
     * (`operator`, `direction`, and `isAlpr = true` if either source identifies it as ALPR).
     */
    fun mergeAndDedupe(vararg lists: List<AlprCamera>, radiusM: Double = 35.0): List<AlprCamera> {
        val total = lists.sumOf { it.size }
        if (total == 0) return emptyList()
        val cellDeg = 0.002 // ~220 m
        val r2 = radiusM * radiusM
        val out = ArrayList<AlprCamera>(total)
        val spatial = HashMap<Long, MutableList<Int>>()

        fun cellKey(r: Long, c: Long): Long = (r shl 32) xor (c and 0xffffffffL)

        for (list in lists) {
            for (cam in list) {
                val la = cam.loc.lat
                val lo = cam.loc.lng
                val cr = Math.floor(la / cellDeg).toLong()
                val cc = Math.floor(lo / cellDeg).toLong()
                val cosLat = Math.cos(Math.toRadians(la))
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
                } else {
                    val existing = out[matchIdx]
                    val mergedOp = existing.operator.ifEmpty { cam.operator }
                    val mergedDir = existing.direction.ifEmpty { cam.direction }
                    val mergedAlpr = existing.isAlpr || cam.isAlpr
                    if (mergedOp != existing.operator || mergedDir != existing.direction || mergedAlpr != existing.isAlpr) {
                        out[matchIdx] = AlprCamera(existing.loc, mergedOp, mergedDir, mergedAlpr)
                    }
                }
            }
        }
        return out
    }

    /** Cameras within [meters] of any SEGMENT of [polyline] that can see traffic on it, for the
     *  route count and the nav alerts. Empty if not loaded.
     *  45 m, not the old 120 (issue #527): a camera on a parallel alternate a block over, or on
     *  the frontage road, counted against a route that never passes it, so a camera-free route
     *  badged "1 camera". A roadside ALPR sits within a lane or two of the way it watches, and a
     *  divided highway's far carriageway is mostly past 45 m, so this counts what you drive past.
     *  DIRECTION-AWARE (2026-09-16): a camera with a known facing counts only when that facing is
     *  within 50 degrees of the nearest route segment's axis, either way along it; a plate reader
     *  aimed across the road reads cross traffic. Untagged cameras count. Rule in :core
     *  [app.vela.core.nav.CameraFacing]. */
    fun along(polyline: List<LatLng>, meters: Double = 45.0): List<AlprCamera> {
        if (!loaded || polyline.size < 2) return emptyList()
        val pad = 0.01
        val r0 = rowOf(polyline.minOf { it.lat } - pad); val r1 = rowOf(polyline.maxOf { it.lat } + pad)
        val c0 = rowOf(polyline.minOf { it.lng } - pad); val c1 = rowOf(polyline.maxOf { it.lng } + pad)
        val out = ArrayList<AlprCamera>()
        var r = r0
        while (r <= r1) {
            var c = c0
            while (c <= c1) {
                grid[key(r, c)]?.let { bucket ->
                    for (i in bucket) {
                        val p = LatLng(lat[i], lng[i])
                        val d = dir.getOrElse(i) { Float.NaN }
                        val facing = if (d.isNaN()) null else d.toDouble()
                        val isAlpr = isAlprArr.getOrElse(i) { true }
                        val onRoute = if (isAlpr) {
                            app.vela.core.nav.CameraFacing.onRoute(polyline, p, facing, meters)
                        } else {
                            app.vela.core.data.OverpassAlprCameras.nearPolyline(p, polyline, meters.coerceAtLeast(50.0))
                        }
                        if (onRoute) {
                            out.add(AlprCamera(p, op[i], dirStr(i), isAlpr = isAlpr))
                        }
                    }
                }
                c++
            }
            r++
        }
        return out
    }
}
