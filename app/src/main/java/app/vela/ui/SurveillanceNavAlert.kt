package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * Heads-up for general surveillance cameras (CCTV) coming up on the route while navigating.
 *
 * Two separate opt-ins, both OFF by default: [card] shows the heads-up card, [voice] says
 * "Surveillance camera ahead" through the normal nav voice (so the spoken-directions mute
 * applies).
 */
object SurveillanceNavAlert {
    val card = mutableStateOf(false)
    val voice = mutableStateOf(false)
    val distanceMeters = mutableIntStateOf(0) // 0 = Auto (12s depending on speed)

    val any: Boolean get() = card.value || voice.value

    fun init(context: Context) {
        val p = prefs(context)
        card.value = p.getBoolean(KEY_CARD, false)
        voice.value = p.getBoolean(KEY_VOICE, false)
        distanceMeters.intValue = p.getInt(KEY_DISTANCE, 0)
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
    private const val KEY_CARD = "surveillance_nav_alert_card"
    private const val KEY_VOICE = "surveillance_nav_alert_voice"
    private const val KEY_DISTANCE = "surveillance_nav_alert_distance"
}
