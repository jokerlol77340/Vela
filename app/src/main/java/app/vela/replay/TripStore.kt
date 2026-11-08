package app.vela.replay

import android.content.Context
import android.location.Location
import app.vela.core.model.LatLng
import app.vela.core.model.Route
import app.vela.core.replay.TripLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** One recorded GPS fix along a trip. */
data class TripFix(val lat: Double, val lng: Double, val t: Long, val bearing: Float, val speed: Float)

/** Header for a saved trip — enough to list + replay it. */
data class TripMeta(
    val id: String,
    val label: String,
    val startedAt: Long,
    val fixCount: Int,
    val dest: LatLng?,
    /** Meters covered by the recorded fixes, and the time between the first and last one. */
    val distanceM: Double = 0.0,
    val durationMs: Long = 0L,
)

/**
 * Records navigation trips (the GPS trace + destination) to on-device files, so a
 * drive can be **replayed** later for testing turn-by-turn without driving it again.
 * On from the first-run card for new installs, off by default before it (the "save my trips"
 * switch); strictly local, never uploaded. Old drives can clear themselves ([pruneExpired]).
 *
 * Plain CSV (no JSON dep in `:app`): line 1 = `META,<label>,<startedAt>,<destLat>,<destLng>,
 * <versionCode>`, then one `lat,lng,t,bearing,speed,offRoute,accuracy` per fix. FLIGHT-RECORDER
 * lines (2026-07-16, all appended formats - every parser reads by index/prefix and skips what it
 * doesn't know, so old files and old parsers stay compatible): `S,<epochMs>,<text>` = a line the
 * voice actually SPOKE (the voice-vs-card questions answer themselves); `J,<epochMs>,<frames>,
 * <janky>,<worstMs>` = a 30 s UI-thread frame-pacing sample while navigating (Choreographer
 * cadence - catches UI/vsync stalls, though the GL map thread renders separately); `B,<epochMs>,
 * <pct>` = battery level every ~2 min (drain per drive becomes measurable).
 *
 * Optionally the navigated **route** is saved too (right after META), so a replay drives the
 * *same* blue line the user actually saw — not a fresh re-route — and an offline analysis can
 * regenerate exactly what the cards/voice said at each fix and diff it against the route's real
 * maneuver positions. Those lines are `RP,<encoded-polyline>`, `RD,<distM>,<durS>,<trafficS?>`,
 * and one `M,<type>,<lat>,<lng>,<distM>,<instruction>` per maneuver; [load] skips them (their
 * first field never parses as a latitude).
 */
@Singleton
class TripStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dir: File get() = File(context.filesDir, "trips")

    init {
        // Privacy policy: never store GPS trip traces on disk; purge any legacy folder on startup.
        runCatching { dir.deleteRecursively() }
    }

    /** Trip recording is permanently disabled for privacy; never creates files on disk. */
    fun startTrip(label: String, dest: LatLng?, startedAt: Long): String? = null

    /** No-op: GPS fixes are never persisted to disk. */
    fun record(loc: Location, offRoute: Boolean = false, offRouteHits: Int = 0) = Unit

    /** No-op: flight-recorder events are never persisted to disk. */
    fun note(tag: String, payload: String) = Unit

    /** No-op: navigated routes are never persisted to trip logs. */
    fun saveRoute(route: Route, reason: String = "") = Unit

    /** No-op: no trip is ever recorded. */
    fun finishTrip(): TripMeta? = null

    val isRecording: Boolean get() = false

    fun list(): List<TripMeta> = emptyList()

    fun load(id: String): List<TripFix> = emptyList()

    fun loadRoute(id: String): Route? = null

    fun rawCsv(id: String): String? = null

    fun rename(id: String, label: String): Boolean = false

    fun delete(id: String) {
        runCatching { dir.deleteRecursively() }
    }

    fun pruneExpired(nowMs: Long = System.currentTimeMillis()): Int {
        runCatching { dir.deleteRecursively() }
        return 0
    }
}
