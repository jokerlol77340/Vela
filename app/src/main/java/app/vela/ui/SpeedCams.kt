package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * Whether fixed speed/radar cameras are drawn on the map (issue #229). Same reactive-holder shape
 * as [Flock]; OFF by default - the audience that wants radar warnings (mostly Europe) opts in. The
 * cameras come from the baked road-features region ([app.vela.data.RoadFeatures], checked through
 * MapViewModel.roadFeaturesCover); a live per-viewport Overpass fetch answers only where the manifest
 * has no region.
 */
object SpeedCams {
    val on = mutableStateOf(false)
    internal var syncing = false

    fun init(context: Context) {
        val p = prefs(context)
        on.value = p.getBoolean(KEY, false)
        val pPoi = context.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
        if (pPoi.contains(MapPoiPrefs.KEY_SPEEDCAM_SCALE)) {
            val sc = pPoi.getFloat(MapPoiPrefs.KEY_SPEEDCAM_SCALE, 1.0f)
            if (sc <= 0f) on.value = false
        }
    }

    fun set(context: Context, value: Boolean) {
        on.value = value
        prefs(context).edit().putBoolean(KEY, value).apply()
        if (!syncing) {
            syncing = true
            try {
                if (!value) {
                    MapPoiPrefs.setSpeedcamScale(context, 0f)
                } else if (MapPoiPrefs.speedcamScale.floatValue <= 0f) {
                    MapPoiPrefs.setSpeedcamScale(context, 1.0f)
                }
            } finally {
                syncing = false
            }
        }
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY = "speed_cameras_on"
}
