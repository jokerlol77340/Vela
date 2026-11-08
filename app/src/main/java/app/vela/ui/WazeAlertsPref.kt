package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import app.vela.core.model.LatLng

enum class WazeAlertKind {
    POLICE,
    HAZARD,
    ACCIDENT,
}

data class WazeAlert(
    val id: String,
    val loc: LatLng,
    val kind: WazeAlertKind,
    val subtype: String = "",
    val street: String = "",
)

/**
 * Preferences and live route state for one-shot Waze LiveMap alerts along the active route.
 * Deduplicated against fixed speed cameras and ALPR/surveillance cameras so radars are never
 * shown or warned twice.
 */
object WazeAlertsPref {
    val enabled = mutableStateOf(true)
    val showPolice = mutableStateOf(true)
    val showHazards = mutableStateOf(true)
    val showAccidents = mutableStateOf(true)
    val voiceWarn = mutableStateOf(true)
    val cardWarn = mutableStateOf(true)
    val any: Boolean get() = enabled.value && (cardWarn.value || voiceWarn.value)
    val wazeIconScale = mutableFloatStateOf(1.0f)
    val wazeMinZoom = mutableFloatStateOf(0f)
    val wazeShowInNav = mutableStateOf(true)

    /** Raw Waze alerts fetched once for the active route (already deduplicated against fixed radars/cameras). */
    val currentRouteAlerts = mutableStateOf<List<WazeAlert>>(emptyList())

    /** Filtered alerts according to user's category toggles. */
    val filteredRouteAlerts: List<WazeAlert>
        get() {
            if (!enabled.value) return emptyList()
            val p = showPolice.value
            val h = showHazards.value
            val a = showAccidents.value
            return currentRouteAlerts.value.filter { alert ->
                when (alert.kind) {
                    WazeAlertKind.POLICE -> p
                    WazeAlertKind.HAZARD -> h
                    WazeAlertKind.ACCIDENT -> a
                }
            }
        }

    fun init(context: Context) {
        val p = prefs(context)
        enabled.value = p.getBoolean(KEY_ENABLED, true)
        showPolice.value = p.getBoolean(KEY_POLICE, true)
        showHazards.value = p.getBoolean(KEY_HAZARDS, true)
        showAccidents.value = p.getBoolean(KEY_ACCIDENTS, true)
        voiceWarn.value = p.getBoolean(KEY_VOICE, true)
        cardWarn.value = p.getBoolean(KEY_CARD, true)
        wazeIconScale.floatValue = p.getFloat(KEY_SCALE, 1.0f)
        wazeMinZoom.floatValue = p.getFloat(KEY_MIN_ZOOM, 0f)
        wazeShowInNav.value = p.getBoolean(KEY_SHOW_IN_NAV, true)
    }

    fun setWazeMinZoom(context: Context, value: Float) {
        wazeMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_MIN_ZOOM, value).apply()
    }

    fun setWazeShowInNav(context: Context, value: Boolean) {
        wazeShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_SHOW_IN_NAV, value).apply()
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled.value = value
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
        if (!value) currentRouteAlerts.value = emptyList()
    }

    fun setShowPolice(context: Context, value: Boolean) {
        showPolice.value = value
        prefs(context).edit().putBoolean(KEY_POLICE, value).apply()
    }

    fun setShowHazards(context: Context, value: Boolean) {
        showHazards.value = value
        prefs(context).edit().putBoolean(KEY_HAZARDS, value).apply()
    }

    fun setShowAccidents(context: Context, value: Boolean) {
        showAccidents.value = value
        prefs(context).edit().putBoolean(KEY_ACCIDENTS, value).apply()
    }

    fun setVoiceWarn(context: Context, value: Boolean) {
        voiceWarn.value = value
        prefs(context).edit().putBoolean(KEY_VOICE, value).apply()
    }

    fun setCardWarn(context: Context, value: Boolean) {
        cardWarn.value = value
        prefs(context).edit().putBoolean(KEY_CARD, value).apply()
    }

    fun setWazeIconScale(context: Context, value: Float) {
        wazeIconScale.floatValue = value
        prefs(context).edit().putFloat(KEY_SCALE, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY_ENABLED = "waze_live_alerts_enabled"
    private const val KEY_POLICE = "waze_live_police"
    private const val KEY_HAZARDS = "waze_live_hazards"
    private const val KEY_ACCIDENTS = "waze_live_accidents"
    private const val KEY_VOICE = "waze_live_voice"
    private const val KEY_CARD = "waze_live_card"
    private const val KEY_SCALE = "map_nav_waze_scale"
    private const val KEY_MIN_ZOOM = "map_nav_waze_min_zoom"
    private const val KEY_SHOW_IN_NAV = "map_nav_waze_show_in_nav"
}
