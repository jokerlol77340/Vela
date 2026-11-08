package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import app.vela.core.data.AlprCamera
import app.vela.core.model.TravelMode

/**
 * Which class of cameras to avoid when computing and ranking routes.
 */
enum class CameraAvoidType(val prefKey: String) {
    NONE("none"),
    ALPR_ONLY("alpr"),
    SURVEILLANCE_ONLY("surveillance"),
    ALL_CAMERAS("all");

    companion object {
        fun fromKey(key: String?): CameraAvoidType =
            entries.firstOrNull { it.prefKey == key } ?: NONE
    }
}

/**
 * Per-travel-mode camera avoidance configuration (`DRIVE`, `WALK`, `BICYCLE`), allowing the user
 * to choose independently for each mode whether to avoid:
 * - [CameraAvoidType.NONE] (no cameras)
 * - [CameraAvoidType.ALPR_ONLY] (license-plate readers only)
 * - [CameraAvoidType.SURVEILLANCE_ONLY] (general surveillance cameras only)
 * - [CameraAvoidType.ALL_CAMERAS] (all cameras: plate readers + surveillance + speed cameras)
 */
object FlockRouteAlert {
    val on = mutableStateOf(false)
    val driveType = mutableStateOf(CameraAvoidType.NONE)
    val walkType = mutableStateOf(CameraAvoidType.NONE)
    val bikeType = mutableStateOf(CameraAvoidType.NONE)

    fun init(context: Context) {
        val p = prefs(context)
        val legacyOn = p.getBoolean(KEY, false)
        val dDefault = if (legacyOn) CameraAvoidType.ALPR_ONLY.prefKey else CameraAvoidType.NONE.prefKey
        driveType.value = CameraAvoidType.fromKey(p.getString(KEY_DRIVE, dDefault))
        walkType.value = CameraAvoidType.fromKey(p.getString(KEY_WALK, CameraAvoidType.NONE.prefKey))
        bikeType.value = CameraAvoidType.fromKey(p.getString(KEY_BIKE, CameraAvoidType.NONE.prefKey))
        syncMasterFlag()
    }

    fun set(context: Context, value: Boolean) {
        val next = if (value) {
            if (driveType.value != CameraAvoidType.NONE) driveType.value else CameraAvoidType.ALL_CAMERAS
        } else {
            CameraAvoidType.NONE
        }
        setTypeFor(context, TravelMode.DRIVE, next)
        if (!value) {
            setTypeFor(context, TravelMode.WALK, CameraAvoidType.NONE)
            setTypeFor(context, TravelMode.BICYCLE, CameraAvoidType.NONE)
        }
    }

    fun typeFor(mode: TravelMode): CameraAvoidType = when (mode) {
        TravelMode.DRIVE -> driveType.value
        TravelMode.WALK -> walkType.value
        TravelMode.BICYCLE -> bikeType.value
        else -> CameraAvoidType.NONE
    }

    fun isEnabledFor(mode: TravelMode): Boolean = typeFor(mode) != CameraAvoidType.NONE

    fun setTypeFor(context: Context, mode: TravelMode, type: CameraAvoidType) {
        val p = prefs(context).edit()
        when (mode) {
            TravelMode.DRIVE -> {
                driveType.value = type
                p.putString(KEY_DRIVE, type.prefKey)
            }
            TravelMode.WALK -> {
                walkType.value = type
                p.putString(KEY_WALK, type.prefKey)
            }
            TravelMode.BICYCLE -> {
                bikeType.value = type
                p.putString(KEY_BIKE, type.prefKey)
            }
            else -> {}
        }
        syncMasterFlag()
        p.putBoolean(KEY, on.value).apply()
    }

    fun filterAlprAndSurveillance(cameras: List<AlprCamera>, type: CameraAvoidType): List<AlprCamera> =
        when (type) {
            CameraAvoidType.NONE -> emptyList()
            CameraAvoidType.ALPR_ONLY -> cameras.filter { it.isAlpr }
            CameraAvoidType.SURVEILLANCE_ONLY -> cameras.filter { !it.isAlpr }
            CameraAvoidType.ALL_CAMERAS -> cameras
        }

    fun includeSpeedCameras(type: CameraAvoidType): Boolean =
        type == CameraAvoidType.ALL_CAMERAS

    private fun syncMasterFlag() {
        on.value = driveType.value != CameraAvoidType.NONE ||
            walkType.value != CameraAvoidType.NONE ||
            bikeType.value != CameraAvoidType.NONE
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "flock_route_alert_on"
    private const val KEY_DRIVE = "flock_avoid_type_drive"
    private const val KEY_WALK = "flock_avoid_type_walk"
    private const val KEY_BIKE = "flock_avoid_type_bike"
}
