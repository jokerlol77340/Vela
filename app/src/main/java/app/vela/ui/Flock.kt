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

    fun init(context: Context) {
        val p = prefs(context)
        val master = p.getBoolean(KEY, true)
        showAlpr.value = p.getBoolean(KEY_ALPR, master)
        showSurveillance.value = p.getBoolean(KEY_SURV, master)
        on.value = showAlpr.value || showSurveillance.value
    }

    fun set(context: Context, value: Boolean) {
        showAlpr.value = value
        showSurveillance.value = value
        on.value = value
        prefs(context).edit()
            .putBoolean(KEY, value)
            .putBoolean(KEY_ALPR, value)
            .putBoolean(KEY_SURV, value)
            .apply()
        refreshNow?.invoke()
    }

    fun setShowAlpr(context: Context, value: Boolean) {
        showAlpr.value = value
        on.value = showAlpr.value || showSurveillance.value
        prefs(context).edit()
            .putBoolean(KEY_ALPR, value)
            .putBoolean(KEY, on.value)
            .apply()
        refreshNow?.invoke()
    }

    fun setShowSurveillance(context: Context, value: Boolean) {
        showSurveillance.value = value
        on.value = showAlpr.value || showSurveillance.value
        prefs(context).edit()
            .putBoolean(KEY_SURV, value)
            .putBoolean(KEY, on.value)
            .apply()
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
