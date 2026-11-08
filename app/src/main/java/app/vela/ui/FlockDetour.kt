package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import app.vela.R
import app.vela.core.model.TravelMode

/**
 * Mutually exclusive detour avoidance intensity:
 * - [DISABLED]: Standard route without detours around cameras or radars.
 * - [MODERATE]: Attempts light detours via parallel streets within a reasonable time cap.
 * - [TOTAL_AVOIDANCE]: Aggressively searches for a route passing 0 cameras/radars, even if the trip is long and winding.
 */
enum class CameraAvoidDetourMode(val prefKey: String) {
    DISABLED("disabled"),
    MODERATE("moderate"),
    TOTAL_AVOIDANCE("total_avoidance");

    companion object {
        fun fromKey(key: String?): CameraAvoidDetourMode = when (key?.trim()) {
            "moderate", "true" -> MODERATE
            "total_avoidance" -> TOTAL_AVOIDANCE
            else -> DISABLED
        }
    }
}

/**
 * Camera & radar detour setting. Nested under [FlockRouteAlert] in Settings.
 */
object FlockDetour {
    val mode = mutableStateOf(CameraAvoidDetourMode.DISABLED)
    val on = mutableStateOf(false)

    fun init(context: Context) {
        val p = prefs(context)
        val rawMode = p.getString(KEY_MODE, null)
        val m = if (rawMode != null) {
            CameraAvoidDetourMode.fromKey(rawMode)
        } else {
            if (p.getBoolean(KEY_LEGACY, false)) CameraAvoidDetourMode.MODERATE else CameraAvoidDetourMode.DISABLED
        }
        mode.value = m
        on.value = (m != CameraAvoidDetourMode.DISABLED)
    }

    fun setMode(context: Context, newMode: CameraAvoidDetourMode) {
        mode.value = newMode
        on.value = (newMode != CameraAvoidDetourMode.DISABLED)
        if (newMode == CameraAvoidDetourMode.TOTAL_AVOIDANCE) {
            if (FlockRouteAlert.driveTargets.value.isEmpty()) {
                FlockRouteAlert.setTargetsFor(
                    context,
                    TravelMode.DRIVE,
                    setOf(CameraAvoidTarget.ALPR, CameraAvoidTarget.SURVEILLANCE, CameraAvoidTarget.RADAR)
                )
            }
            if (FlockRouteAlert.walkTargets.value.isEmpty()) {
                FlockRouteAlert.setTargetsFor(
                    context,
                    TravelMode.WALK,
                    setOf(CameraAvoidTarget.ALPR, CameraAvoidTarget.SURVEILLANCE)
                )
            }
            if (FlockRouteAlert.bikeTargets.value.isEmpty()) {
                FlockRouteAlert.setTargetsFor(
                    context,
                    TravelMode.BICYCLE,
                    setOf(CameraAvoidTarget.ALPR, CameraAvoidTarget.SURVEILLANCE)
                )
            }
        }
        prefs(context).edit()
            .putString(KEY_MODE, newMode.prefKey)
            .putBoolean(KEY_LEGACY, on.value)
            .apply()
    }

    fun set(context: Context, value: Boolean) {
        setMode(context, if (value) CameraAvoidDetourMode.MODERATE else CameraAvoidDetourMode.DISABLED)
    }

    /**
     * Build the dynamic label for the moderate detour option based on user-selected targets.
     */
    fun dynamicModerateLabel(context: Context): String {
        val all = FlockRouteAlert.driveTargets.value + FlockRouteAlert.walkTargets.value + FlockRouteAlert.bikeTargets.value
        val hasAlpr = CameraAvoidTarget.ALPR in all
        val hasSurv = CameraAvoidTarget.SURVEILLANCE in all
        val hasRadar = CameraAvoidTarget.RADAR in all

        return when {
            hasRadar && (hasAlpr || hasSurv) -> context.getString(R.string.settings_flock_detour_cams_and_radars)
            hasRadar -> context.getString(R.string.settings_flock_detour_radars_only)
            hasAlpr && hasSurv -> context.getString(R.string.settings_flock_detour_cams_only)
            hasAlpr -> context.getString(R.string.settings_flock_detour_alpr_only)
            hasSurv -> context.getString(R.string.settings_flock_detour_surv_only)
            else -> context.getString(R.string.settings_flock_detour_cams_and_radars)
        }
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY_MODE = "flock_detour_mode"
    private const val KEY_LEGACY = "flock_detour"
}
