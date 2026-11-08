package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import app.vela.core.data.AlprCamera
import app.vela.core.model.TravelMode

/**
 * Individual camera / radar categories that can be avoided simultaneously when computing routes.
 * Note: [RADAR] is only applicable to [TravelMode.DRIVE] (not pedestrian or bicycle modes).
 */
enum class CameraAvoidTarget(val prefKey: String) {
    ALPR("alpr"),
    SURVEILLANCE("surveillance"),
    RADAR("radar");

    companion object {
        fun fromKey(key: String): CameraAvoidTarget? =
            entries.firstOrNull { it.prefKey == key.trim() }

        fun allowedFor(mode: TravelMode): List<CameraAvoidTarget> = when (mode) {
            TravelMode.DRIVE -> listOf(ALPR, SURVEILLANCE, RADAR)
            TravelMode.WALK, TravelMode.BICYCLE -> listOf(ALPR, SURVEILLANCE)
            else -> emptyList()
        }

        fun parseSet(raw: String?, mode: TravelMode): Set<CameraAvoidTarget> {
            if (raw.isNullOrBlank() || raw == "none") return emptySet()
            val allowed = allowedFor(mode).toSet()
            // Legacy migration from single-choice "all"
            if (raw == "all") return allowed
            return raw.split(',')
                .mapNotNull { fromKey(it) }
                .filter { it in allowed }
                .toSet()
        }

        fun serializeSet(targets: Set<CameraAvoidTarget>, mode: TravelMode): String {
            val allowed = allowedFor(mode).toSet()
            val clean = targets.filter { it in allowed }
            return if (clean.isEmpty()) "none" else clean.joinToString(",") { it.prefKey }
        }
    }
}

/**
 * Per-travel-mode multi-select camera & radar avoidance configuration (`DRIVE`, `WALK`, `BICYCLE`).
 * Users can simultaneously check any combination of:
 * - [CameraAvoidTarget.ALPR] ("Caméras lectrices")
 * - [CameraAvoidTarget.SURVEILLANCE] ("Caméras de surveillance")
 * - [CameraAvoidTarget.RADAR] ("Radar" — available for [TravelMode.DRIVE] only)
 */
object FlockRouteAlert {
    val on = mutableStateOf(false)
    val driveTargets = mutableStateOf<Set<CameraAvoidTarget>>(emptySet())
    val walkTargets = mutableStateOf<Set<CameraAvoidTarget>>(emptySet())
    val bikeTargets = mutableStateOf<Set<CameraAvoidTarget>>(emptySet())

    fun init(context: Context) {
        val p = prefs(context)
        val legacyOn = p.getBoolean(KEY, false)
        val dDefault = if (legacyOn) CameraAvoidTarget.ALPR.prefKey else "none"
        val rawDrive = p.getString(KEY_DRIVE_TARGETS, null) ?: p.getString(KEY_DRIVE_LEGACY, dDefault)
        val rawWalk = p.getString(KEY_WALK_TARGETS, null) ?: p.getString(KEY_WALK_LEGACY, "none")
        val rawBike = p.getString(KEY_BIKE_TARGETS, null) ?: p.getString(KEY_BIKE_LEGACY, "none")

        driveTargets.value = CameraAvoidTarget.parseSet(rawDrive, TravelMode.DRIVE)
        walkTargets.value = CameraAvoidTarget.parseSet(rawWalk, TravelMode.WALK)
        bikeTargets.value = CameraAvoidTarget.parseSet(rawBike, TravelMode.BICYCLE)
        syncMasterFlag()
    }

    fun set(context: Context, value: Boolean) {
        if (value) {
            if (driveTargets.value.isEmpty()) {
                setTargetsFor(
                    context,
                    TravelMode.DRIVE,
                    setOf(CameraAvoidTarget.ALPR, CameraAvoidTarget.SURVEILLANCE, CameraAvoidTarget.RADAR),
                )
            }
        } else {
            setTargetsFor(context, TravelMode.DRIVE, emptySet())
            setTargetsFor(context, TravelMode.WALK, emptySet())
            setTargetsFor(context, TravelMode.BICYCLE, emptySet())
        }
    }

    fun targetsFor(mode: TravelMode): Set<CameraAvoidTarget> = when (mode) {
        TravelMode.DRIVE -> driveTargets.value
        TravelMode.WALK -> walkTargets.value
        TravelMode.BICYCLE -> bikeTargets.value
        else -> emptySet()
    }

    fun isEnabledFor(mode: TravelMode): Boolean = targetsFor(mode).isNotEmpty()

    fun toggleTarget(context: Context, mode: TravelMode, target: CameraAvoidTarget) {
        if (target !in CameraAvoidTarget.allowedFor(mode)) return
        val cur = targetsFor(mode)
        val next = if (target in cur) cur - target else cur + target
        setTargetsFor(context, mode, next)
    }

    fun setTargetsFor(context: Context, mode: TravelMode, targets: Set<CameraAvoidTarget>) {
        val allowed = CameraAvoidTarget.allowedFor(mode).toSet()
        val clean = targets.filter { it in allowed }.toSet()
        val serialized = CameraAvoidTarget.serializeSet(clean, mode)
        val p = prefs(context).edit()
        when (mode) {
            TravelMode.DRIVE -> {
                driveTargets.value = clean
                p.putString(KEY_DRIVE_TARGETS, serialized)
            }
            TravelMode.WALK -> {
                walkTargets.value = clean
                p.putString(KEY_WALK_TARGETS, serialized)
            }
            TravelMode.BICYCLE -> {
                bikeTargets.value = clean
                p.putString(KEY_BIKE_TARGETS, serialized)
            }
            else -> {}
        }
        syncMasterFlag()
        p.putBoolean(KEY, on.value).apply()
    }

    fun filterAlprAndSurveillance(cameras: List<AlprCamera>, targets: Set<CameraAvoidTarget>): List<AlprCamera> {
        val wantAlpr = CameraAvoidTarget.ALPR in targets
        val wantSurv = CameraAvoidTarget.SURVEILLANCE in targets
        return when {
            wantAlpr && wantSurv -> cameras
            wantAlpr -> cameras.filter { it.isAlpr }
            wantSurv -> cameras.filter { !it.isAlpr }
            else -> emptyList()
        }
    }

    fun includeSpeedCameras(mode: TravelMode, targets: Set<CameraAvoidTarget>): Boolean =
        mode == TravelMode.DRIVE && CameraAvoidTarget.RADAR in targets

    private fun syncMasterFlag() {
        on.value = driveTargets.value.isNotEmpty() ||
            walkTargets.value.isNotEmpty() ||
            bikeTargets.value.isNotEmpty()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "flock_route_alert_on"
    private const val KEY_DRIVE_LEGACY = "flock_avoid_type_drive"
    private const val KEY_WALK_LEGACY = "flock_avoid_type_walk"
    private const val KEY_BIKE_LEGACY = "flock_avoid_type_bike"
    private const val KEY_DRIVE_TARGETS = "flock_avoid_targets_drive"
    private const val KEY_WALK_TARGETS = "flock_avoid_targets_walk"
    private const val KEY_BIKE_TARGETS = "flock_avoid_targets_bike"
}
