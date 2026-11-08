package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import app.vela.core.data.AlprCamera

/**
 * Whether ALPR ("Flock") plate-reader cameras and/or general surveillance cameras are drawn on the map.
 * Both are enabled by default and can be toggled independently in Settings > Navigation > Alerts.
 */
object Flock {
    val on = mutableStateOf(true)
    val showAlpr = mutableStateOf(true)
    val showSurveillance = mutableStateOf(true)
    var refreshNow: (() -> Unit)? = null
    internal var syncing = false

    fun init(context: Context) {
        val p = prefs(context)
        val master = p.getBoolean(KEY, true)
        showAlpr.value = p.getBoolean(KEY_ALPR, master)
        showSurveillance.value = p.getBoolean(KEY_SURV, master)
        val pPoi = context.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
        if (pPoi.contains(MapPoiPrefs.KEY_FLOCK_SCALE)) {
            val sc = pPoi.getFloat(MapPoiPrefs.KEY_FLOCK_SCALE, 1.0f)
            if (sc <= 0f) showAlpr.value = false
        }
        if (pPoi.contains(MapPoiPrefs.KEY_SURV_CAM_SCALE)) {
            val sc = pPoi.getFloat(MapPoiPrefs.KEY_SURV_CAM_SCALE, 1.0f)
            if (sc <= 0f) showSurveillance.value = false
        }
        on.value = showAlpr.value || showSurveillance.value
    }

    fun set(context: Context, value: Boolean) {
        setShowAlpr(context, value)
        setShowSurveillance(context, value)
    }

    fun setShowAlpr(context: Context, value: Boolean) {
        showAlpr.value = value
        on.value = showAlpr.value || showSurveillance.value
        prefs(context).edit()
            .putBoolean(KEY_ALPR, value)
            .putBoolean(KEY, on.value)
            .apply()
        if (!syncing) {
            syncing = true
            try {
                if (!value) {
                    MapPoiPrefs.setFlockScale(context, 0f)
                } else if (MapPoiPrefs.flockScale.floatValue <= 0f) {
                    MapPoiPrefs.setFlockScale(context, 1.0f)
                }
            } finally {
                syncing = false
            }
        }
        refreshNow?.invoke()
    }

    fun setShowSurveillance(context: Context, value: Boolean) {
        showSurveillance.value = value
        on.value = showAlpr.value || showSurveillance.value
        prefs(context).edit()
            .putBoolean(KEY_SURV, value)
            .putBoolean(KEY, on.value)
            .apply()
        if (!syncing) {
            syncing = true
            try {
                if (!value) {
                    MapPoiPrefs.setSurveillanceCamScale(context, 0f)
                } else if (MapPoiPrefs.surveillanceCamScale.floatValue <= 0f) {
                    MapPoiPrefs.setSurveillanceCamScale(context, 1.0f)
                }
            } finally {
                syncing = false
            }
        }
        refreshNow?.invoke()
    }

    fun filterEnabled(cameras: List<AlprCamera>): List<AlprCamera> {
        val alpr = showAlpr.value
        val surv = showSurveillance.value
        return when {
            alpr && surv -> cameras
            alpr -> cameras.filter { it.isAlpr }
            surv -> cameras.filter { !it.isAlpr }
            else -> emptyList()
        }
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "flock_cameras_on"
    private const val KEY_ALPR = "flock_alpr_cameras_on"
    private const val KEY_SURV = "flock_surv_cameras_on"
}
