package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * Radar / speed camera alerts during navigation (issue #229).
 *
 * Supports audio announcement ([voice]), visual card ([card]), and customizable distance
 * in meters ([distanceMeters] = 0 for auto / 12 seconds, or specific meters e.g. 200, 400, 600, 800, 1000).
 */
object SpeedCamWarn {
    val on = mutableStateOf(false)
    val card = mutableStateOf(true)
    val voice = mutableStateOf(false)
    val distanceMeters = mutableIntStateOf(0) // 0 = Auto (12s depending on speed)

    val any: Boolean get() = on.value && (card.value || voice.value)

    fun init(context: Context) {
        val p = prefs(context)
        on.value = p.getBoolean(KEY, false)
        card.value = p.getBoolean(KEY_CARD, true)
        voice.value = p.getBoolean(KEY_VOICE, on.value)
        distanceMeters.intValue = p.getInt(KEY_DISTANCE, 0)
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
    }

    fun setCard(context: Context, value: Boolean) {
        card.value = value
        prefs(context).edit().putBoolean(KEY_CARD, value).apply()
    }

    fun setVoice(context: Context, value: Boolean) {
        voice.value = value
        prefs(context).edit().putBoolean(KEY_VOICE, value).apply()
    }

    fun setDistanceMeters(context: Context, meters: Int) {
        distanceMeters.intValue = meters
        prefs(context).edit().putInt(KEY_DISTANCE, meters).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "speed_cam_warn"
    private const val KEY_CARD = "speed_cam_warn_card"
    private const val KEY_VOICE = "speed_cam_warn_voice"
    private const val KEY_DISTANCE = "speed_cam_warn_distance"
}
