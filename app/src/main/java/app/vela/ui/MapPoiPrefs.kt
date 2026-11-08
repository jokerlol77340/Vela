package app.vela.ui

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf

/**
 * Map-POI visibility + sizing preferences (user 2026-07-15). Same reactive-object pattern as
 * [Buildings3d]: Compose state the map layer reads live, persisted in vela_settings.
 *
 * - [showPois]: master switch for EVERY business layer on the browse map: the ambient place
 *   layer, the open (Overture) places layer, and the OSM fallback business POIs. It predates the
 *   open layer, and forgetting to extend it there was issue #597 - once the open source became the
 *   default, turning the switch off still left those places drawn.
 *   Off = a clean basemap; searched results, transit stops and traffic controls still show
 *   (they have their own switches/behavior).
 * - [showTransit]: the canonical GTFS stop icons (and their per-viewport fetch).
 * - [showCivic]: parks, schools and civic places inside the ambient pool - the "not really a
 *   business" tier; off = the ambient layer shows businesses only.
 * - [iconScale]: multiplies the POI icon/label sizes on the map. Exists for low-density screens
 *   (a 1024x600 car head unit renders the fixed-px bitmaps physically huge); phones stay at 1.0.
 */
object MapPoiPrefs {
    val showPois = mutableStateOf(true)
    val showTransit = mutableStateOf(true)
    val showCivic = mutableStateOf(true)
    val iconScale = mutableFloatStateOf(1.0f)
    /** Individual multipliers for each route/navigation pictogram on the map. */
    val speedcamScale = mutableFloatStateOf(1.0f)
    val flockScale = mutableFloatStateOf(1.0f)
    val surveillanceCamScale = mutableFloatStateOf(1.0f)
    val signalScale = mutableFloatStateOf(1.0f)
    val stopSignScale = mutableFloatStateOf(1.0f)
    val railCrossingScale = mutableFloatStateOf(1.0f)
    val speedHumpScale = mutableFloatStateOf(1.0f)
    /** Individual multipliers for road and map pictograms. */
    val roadShieldScale = mutableFloatStateOf(1.0f)
    val exitNumberScale = mutableFloatStateOf(1.0f)
    val fuelStationScale = mutableFloatStateOf(1.0f)
    val transitStopScale = mutableFloatStateOf(1.0f)
    val poiPlaceScale = mutableFloatStateOf(1.0f)
    val parkingScale = mutableFloatStateOf(1.0f)
    val savedPinScale = mutableFloatStateOf(1.0f)

    /** Min zoom levels per pictogram (0f = default system zoom floor). */
    val speedcamMinZoom = mutableFloatStateOf(0f)
    val flockMinZoom = mutableFloatStateOf(0f)
    val surveillanceCamMinZoom = mutableFloatStateOf(0f)
    val signalMinZoom = mutableFloatStateOf(0f)
    val stopSignMinZoom = mutableFloatStateOf(0f)
    val railCrossingMinZoom = mutableFloatStateOf(0f)
    val speedHumpMinZoom = mutableFloatStateOf(0f)
    val roadShieldMinZoom = mutableFloatStateOf(0f)
    val exitNumberMinZoom = mutableFloatStateOf(0f)
    val fuelStationMinZoom = mutableFloatStateOf(0f)
    val transitStopMinZoom = mutableFloatStateOf(0f)
    val poiPlaceMinZoom = mutableFloatStateOf(0f)
    val parkingMinZoom = mutableFloatStateOf(0f)
    val savedPinMinZoom = mutableFloatStateOf(0f)

    /** Display mode per pictogram: true = Carte & Navigation, false = Carte uniquement. */
    val speedcamShowInNav = mutableStateOf(true)
    val flockShowInNav = mutableStateOf(true)
    val surveillanceCamShowInNav = mutableStateOf(true)
    val signalShowInNav = mutableStateOf(true)
    val stopSignShowInNav = mutableStateOf(true)
    val railCrossingShowInNav = mutableStateOf(true)
    val speedHumpShowInNav = mutableStateOf(true)
    val roadShieldShowInNav = mutableStateOf(false)
    val exitNumberShowInNav = mutableStateOf(false)
    val fuelStationShowInNav = mutableStateOf(true)
    val transitStopShowInNav = mutableStateOf(false)
    val poiPlaceShowInNav = mutableStateOf(false)
    val parkingShowInNav = mutableStateOf(true)
    val savedPinShowInNav = mutableStateOf(true)
    /** Where the businesses on the browse map come from, one of [SOURCE_OPEN] (the Overture places
     *  layer where a region file covers the view, Google only on tap, works offline), [SOURCE_GOOGLE]
     *  (the ambient fan-out on every pan, nothing offline) or [SOURCE_BOTH] (the open layer draws the
     *  map and one Google fetch per settled view fills in what it lacks). Outside any region file all
     *  three behave like Google. The user picks; each option's costs are stated in Settings. */
    val placesSource = mutableStateOf(SOURCE_OPEN)
    /** The user's own pick, or null while they have never touched the picker (the fleet default,
     *  `Calibration.defaultPlacesSource`, applies then and can be flipped remotely). */
    private var explicitSource: String? = null
    private var remoteDefault: String = SOURCE_OPEN
    /** The open places layer is on the map (open data or both). */
    val openPlaces: Boolean get() = placesSource.value != SOURCE_GOOGLE
    /** The open places layer alone owns the map's businesses where it covers the view. */
    val openPlacesOnly: Boolean get() = placesSource.value == SOURCE_OPEN
    /** A region download also pulls the Vela places archive covering it (on by default), so the
     *  map's businesses draw offline. Off keeps places streaming-only, which is free when online. */
    val placesWithDownloads = mutableStateOf(true)
    /** Tapping a place on the Vela data layer looks its listing up on Google (hours, reviews, photos).
     *  Off: the sheet shows only what the tile carries and nothing about the tap reaches Google, for
     *  people who want Google kept to search and directions. */
    val lookupTappedPlaces = mutableStateOf(true)
    /** Vela data mode: also draw the shops, restaurants and other businesses mapped in
     *  OpenStreetMap (they are already in the streamed basemap tiles, hidden by default because
     *  the baked places cover businesses better). Doubles of a Vela place are dropped by name.
     *  ON by default since 2026-09-17: what OSM adds is what nothing else has, and a double is
     *  dropped anyway, so the only cost is OSM's own stale rows. */
    val osmBusinesses = mutableStateOf(true) // no longer a setting (2026-09-23): always on for older archives

    fun init(context: Context) {
        val p = prefs(context)
        showPois.value = p.getBoolean(KEY_POIS, true)
        showTransit.value = p.getBoolean(KEY_TRANSIT, true)
        showCivic.value = p.getBoolean(KEY_CIVIC, true)
        iconScale.floatValue = p.getFloat(KEY_SCALE, 1.0f)
        speedcamScale.floatValue = p.getFloat(KEY_SPEEDCAM_SCALE, 1.0f)
        flockScale.floatValue = p.getFloat(KEY_FLOCK_SCALE, 1.0f)
        surveillanceCamScale.floatValue = p.getFloat(KEY_SURV_CAM_SCALE, 1.0f)

        if (!Flock.showAlpr.value && flockScale.floatValue > 0f) flockScale.floatValue = 0f
        else if (Flock.showAlpr.value && flockScale.floatValue <= 0f) Flock.showAlpr.value = false

        if (!Flock.showSurveillance.value && surveillanceCamScale.floatValue > 0f) surveillanceCamScale.floatValue = 0f
        else if (Flock.showSurveillance.value && surveillanceCamScale.floatValue <= 0f) Flock.showSurveillance.value = false

        if (!SpeedCams.on.value && speedcamScale.floatValue > 0f) speedcamScale.floatValue = 0f
        else if (SpeedCams.on.value && speedcamScale.floatValue <= 0f) SpeedCams.on.value = false
        signalScale.floatValue = p.getFloat(KEY_SIGNAL_SCALE, 1.0f)
        stopSignScale.floatValue = p.getFloat(KEY_STOP_SCALE, 1.0f)
        railCrossingScale.floatValue = p.getFloat(KEY_RAILX_SCALE, 1.0f)
        speedHumpScale.floatValue = p.getFloat(KEY_HUMP_SCALE, 1.0f)
        roadShieldScale.floatValue = p.getFloat(KEY_ROAD_SHIELD_SCALE, 1.0f)
        exitNumberScale.floatValue = p.getFloat(KEY_EXIT_NUMBER_SCALE, 1.0f)
        fuelStationScale.floatValue = p.getFloat(KEY_FUEL_STATION_SCALE, 1.0f)
        transitStopScale.floatValue = p.getFloat(KEY_TRANSIT_STOP_SCALE, 1.0f)
        poiPlaceScale.floatValue = p.getFloat(KEY_POI_PLACE_SCALE, 1.0f)
        parkingScale.floatValue = p.getFloat(KEY_PARKING_SCALE, 1.0f)
        savedPinScale.floatValue = p.getFloat(KEY_SAVED_PIN_SCALE, 1.0f)
        speedcamMinZoom.floatValue = p.getFloat(KEY_SPEEDCAM_MIN_ZOOM, 0f)
        flockMinZoom.floatValue = p.getFloat(KEY_FLOCK_MIN_ZOOM, 0f)
        surveillanceCamMinZoom.floatValue = p.getFloat(KEY_SURV_CAM_MIN_ZOOM, 0f)
        signalMinZoom.floatValue = p.getFloat(KEY_SIGNAL_MIN_ZOOM, 0f)
        stopSignMinZoom.floatValue = p.getFloat(KEY_STOP_MIN_ZOOM, 0f)
        railCrossingMinZoom.floatValue = p.getFloat(KEY_RAILX_MIN_ZOOM, 0f)
        speedHumpMinZoom.floatValue = p.getFloat(KEY_HUMP_MIN_ZOOM, 0f)
        roadShieldMinZoom.floatValue = p.getFloat(KEY_ROAD_SHIELD_MIN_ZOOM, 0f)
        exitNumberMinZoom.floatValue = p.getFloat(KEY_EXIT_NUMBER_MIN_ZOOM, 0f)
        fuelStationMinZoom.floatValue = p.getFloat(KEY_FUEL_STATION_MIN_ZOOM, 0f)
        transitStopMinZoom.floatValue = p.getFloat(KEY_TRANSIT_STOP_MIN_ZOOM, 0f)
        poiPlaceMinZoom.floatValue = p.getFloat(KEY_POI_PLACE_MIN_ZOOM, 0f)
        parkingMinZoom.floatValue = p.getFloat(KEY_PARKING_MIN_ZOOM, 0f)
        savedPinMinZoom.floatValue = p.getFloat(KEY_SAVED_PIN_MIN_ZOOM, 0f)

        speedcamShowInNav.value = p.getBoolean(KEY_SPEEDCAM_NAV, true)
        flockShowInNav.value = p.getBoolean(KEY_FLOCK_NAV, true)
        surveillanceCamShowInNav.value = p.getBoolean(KEY_SURV_CAM_NAV, true)
        signalShowInNav.value = p.getBoolean(KEY_SIGNAL_NAV, true)
        stopSignShowInNav.value = p.getBoolean(KEY_STOP_NAV, true)
        railCrossingShowInNav.value = p.getBoolean(KEY_RAILX_NAV, true)
        speedHumpShowInNav.value = p.getBoolean(KEY_HUMP_NAV, true)
        roadShieldShowInNav.value = p.getBoolean(KEY_ROAD_SHIELD_NAV, false)
        exitNumberShowInNav.value = p.getBoolean(KEY_EXIT_NUMBER_NAV, false)
        fuelStationShowInNav.value = p.getBoolean(KEY_FUEL_STATION_NAV, true)
        transitStopShowInNav.value = p.getBoolean(KEY_TRANSIT_STOP_NAV, false)
        poiPlaceShowInNav.value = p.getBoolean(KEY_POI_PLACE_NAV, false)
        parkingShowInNav.value = p.getBoolean(KEY_PARKING_NAV, true)
        savedPinShowInNav.value = p.getBoolean(KEY_SAVED_PIN_NAV, true)
        explicitSource = p.getString(KEY_PLACES_SOURCE, null)
        placesSource.value = explicitSource ?: remoteDefault
        placesWithDownloads.value = p.getBoolean(KEY_PLACES_WITH_DOWNLOADS, true)
        lookupTappedPlaces.value = p.getBoolean(KEY_LOOKUP_TAPPED, true)
        navTapPlaces.value = p.getBoolean(KEY_NAV_TAP_PLACES, false)
    }

    fun setLookupTappedPlaces(context: Context, value: Boolean) {
        lookupTappedPlaces.value = value
        prefs(context).edit().putBoolean(KEY_LOOKUP_TAPPED, value).apply()
    }

    /** Drive navigation: show the places you would divert for (fuel, food, coffee, charging) and let
     *  a tap on one offer it as a stop. Off by default - nav hides places on purpose, both for the
     *  frame rate and for a readable map. Turning it on needs the master places switch, so it turns
     *  that on too and remembers it did, and putting it back off undoes exactly that. */
    val navTapPlaces = mutableStateOf(false)

    fun setNavTapPlaces(context: Context, value: Boolean) {
        navTapPlaces.value = value
        val p = prefs(context)
        val e = p.edit().putBoolean(KEY_NAV_TAP_PLACES, value)
        if (value && !showPois.value) {
            showPois.value = true
            e.putBoolean(KEY_POIS, true).putBoolean(KEY_NAV_TAP_FORCED_POIS, true)
        } else if (!value && p.getBoolean(KEY_NAV_TAP_FORCED_POIS, false)) {
            showPois.value = false
            e.putBoolean(KEY_POIS, false).putBoolean(KEY_NAV_TAP_FORCED_POIS, false)
        }
        e.apply()
    }

    fun setPlacesWithDownloads(context: Context, value: Boolean) {
        placesWithDownloads.value = value
        prefs(context).edit().putBoolean(KEY_PLACES_WITH_DOWNLOADS, value).apply()
    }

    fun setPlacesSource(context: Context, value: String) {
        explicitSource = value
        placesSource.value = value
        prefs(context).edit().putString(KEY_PLACES_SOURCE, value).apply()
    }

    /** The signed bundle's fleet default (MapViewModel pushes it at init and after each refresh);
     *  takes effect only for people who never made their own pick. */
    fun setRemoteDefault(value: String) {
        remoteDefault = value
        if (explicitSource == null) placesSource.value = value
    }

    fun setShowPois(context: Context, value: Boolean) {
        showPois.value = value
        prefs(context).edit().putBoolean(KEY_POIS, value).apply()
    }

    fun setShowTransit(context: Context, value: Boolean) {
        showTransit.value = value
        prefs(context).edit().putBoolean(KEY_TRANSIT, value).apply()
    }

    fun setShowCivic(context: Context, value: Boolean) {
        showCivic.value = value
        prefs(context).edit().putBoolean(KEY_CIVIC, value).apply()
    }

    fun setIconScale(context: Context, value: Float) {
        iconScale.floatValue = value
        prefs(context).edit().putFloat(KEY_SCALE, value).apply()
    }

    fun setSpeedcamScale(context: Context, value: Float) {
        speedcamScale.floatValue = value
        prefs(context).edit().putFloat(KEY_SPEEDCAM_SCALE, value).apply()
        if (!SpeedCams.syncing) {
            SpeedCams.syncing = true
            try {
                if (value <= 0f && SpeedCams.on.value) {
                    SpeedCams.set(context, false)
                } else if (value > 0f && !SpeedCams.on.value) {
                    SpeedCams.set(context, true)
                }
            } finally {
                SpeedCams.syncing = false
            }
        }
    }

    fun setFlockScale(context: Context, value: Float) {
        flockScale.floatValue = value
        prefs(context).edit().putFloat(KEY_FLOCK_SCALE, value).apply()
        if (!Flock.syncing) {
            Flock.syncing = true
            try {
                if (value <= 0f && Flock.showAlpr.value) {
                    Flock.setShowAlpr(context, false)
                } else if (value > 0f && !Flock.showAlpr.value) {
                    Flock.setShowAlpr(context, true)
                }
            } finally {
                Flock.syncing = false
            }
        }
    }

    fun setSurveillanceCamScale(context: Context, value: Float) {
        surveillanceCamScale.floatValue = value
        prefs(context).edit().putFloat(KEY_SURV_CAM_SCALE, value).apply()
        if (!Flock.syncing) {
            Flock.syncing = true
            try {
                if (value <= 0f && Flock.showSurveillance.value) {
                    Flock.setShowSurveillance(context, false)
                } else if (value > 0f && !Flock.showSurveillance.value) {
                    Flock.setShowSurveillance(context, true)
                }
            } finally {
                Flock.syncing = false
            }
        }
    }

    fun setSignalScale(context: Context, value: Float) {
        signalScale.floatValue = value
        prefs(context).edit().putFloat(KEY_SIGNAL_SCALE, value).apply()
    }

    fun setStopSignScale(context: Context, value: Float) {
        stopSignScale.floatValue = value
        prefs(context).edit().putFloat(KEY_STOP_SCALE, value).apply()
    }

    fun setRailCrossingScale(context: Context, value: Float) {
        railCrossingScale.floatValue = value
        prefs(context).edit().putFloat(KEY_RAILX_SCALE, value).apply()
    }

    fun setSpeedHumpScale(context: Context, value: Float) {
        speedHumpScale.floatValue = value
        prefs(context).edit().putFloat(KEY_HUMP_SCALE, value).apply()
    }

    fun setRoadShieldScale(context: Context, value: Float) {
        roadShieldScale.floatValue = value
        prefs(context).edit().putFloat(KEY_ROAD_SHIELD_SCALE, value).apply()
    }

    fun setExitNumberScale(context: Context, value: Float) {
        exitNumberScale.floatValue = value
        prefs(context).edit().putFloat(KEY_EXIT_NUMBER_SCALE, value).apply()
    }

    fun setFuelStationScale(context: Context, value: Float) {
        fuelStationScale.floatValue = value
        prefs(context).edit().putFloat(KEY_FUEL_STATION_SCALE, value).apply()
    }

    fun setTransitStopScale(context: Context, value: Float) {
        transitStopScale.floatValue = value
        prefs(context).edit().putFloat(KEY_TRANSIT_STOP_SCALE, value).apply()
    }

    fun setPoiPlaceScale(context: Context, value: Float) {
        poiPlaceScale.floatValue = value
        prefs(context).edit().putFloat(KEY_POI_PLACE_SCALE, value).apply()
    }

    fun setParkingScale(context: Context, value: Float) {
        parkingScale.floatValue = value
        prefs(context).edit().putFloat(KEY_PARKING_SCALE, value).apply()
    }

    fun setSavedPinScale(context: Context, value: Float) {
        savedPinScale.floatValue = value
        prefs(context).edit().putFloat(KEY_SAVED_PIN_SCALE, value).apply()
    }

    fun setSpeedcamMinZoom(context: Context, value: Float) {
        speedcamMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_SPEEDCAM_MIN_ZOOM, value).apply()
    }

    fun setFlockMinZoom(context: Context, value: Float) {
        flockMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_FLOCK_MIN_ZOOM, value).apply()
    }

    fun setSurveillanceCamMinZoom(context: Context, value: Float) {
        surveillanceCamMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_SURV_CAM_MIN_ZOOM, value).apply()
    }

    fun setSignalMinZoom(context: Context, value: Float) {
        signalMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_SIGNAL_MIN_ZOOM, value).apply()
    }

    fun setStopSignMinZoom(context: Context, value: Float) {
        stopSignMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_STOP_MIN_ZOOM, value).apply()
    }

    fun setRailCrossingMinZoom(context: Context, value: Float) {
        railCrossingMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_RAILX_MIN_ZOOM, value).apply()
    }

    fun setSpeedHumpMinZoom(context: Context, value: Float) {
        speedHumpMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_HUMP_MIN_ZOOM, value).apply()
    }

    fun setRoadShieldMinZoom(context: Context, value: Float) {
        roadShieldMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_ROAD_SHIELD_MIN_ZOOM, value).apply()
    }

    fun setExitNumberMinZoom(context: Context, value: Float) {
        exitNumberMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_EXIT_NUMBER_MIN_ZOOM, value).apply()
    }

    fun setFuelStationMinZoom(context: Context, value: Float) {
        fuelStationMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_FUEL_STATION_MIN_ZOOM, value).apply()
    }

    fun setTransitStopMinZoom(context: Context, value: Float) {
        transitStopMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_TRANSIT_STOP_MIN_ZOOM, value).apply()
    }

    fun setPoiPlaceMinZoom(context: Context, value: Float) {
        poiPlaceMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_POI_PLACE_MIN_ZOOM, value).apply()
    }

    fun setParkingMinZoom(context: Context, value: Float) {
        parkingMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_PARKING_MIN_ZOOM, value).apply()
    }

    fun setSavedPinMinZoom(context: Context, value: Float) {
        savedPinMinZoom.floatValue = value
        prefs(context).edit().putFloat(KEY_SAVED_PIN_MIN_ZOOM, value).apply()
    }

    fun setSpeedcamShowInNav(context: Context, value: Boolean) {
        speedcamShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_SPEEDCAM_NAV, value).apply()
    }

    fun setFlockShowInNav(context: Context, value: Boolean) {
        flockShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_FLOCK_NAV, value).apply()
    }

    fun setSurveillanceCamShowInNav(context: Context, value: Boolean) {
        surveillanceCamShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_SURV_CAM_NAV, value).apply()
    }

    fun setSignalShowInNav(context: Context, value: Boolean) {
        signalShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_SIGNAL_NAV, value).apply()
    }

    fun setStopSignShowInNav(context: Context, value: Boolean) {
        stopSignShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_STOP_NAV, value).apply()
    }

    fun setRailCrossingShowInNav(context: Context, value: Boolean) {
        railCrossingShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_RAILX_NAV, value).apply()
    }

    fun setSpeedHumpShowInNav(context: Context, value: Boolean) {
        speedHumpShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_HUMP_NAV, value).apply()
    }

    fun setRoadShieldShowInNav(context: Context, value: Boolean) {
        roadShieldShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_ROAD_SHIELD_NAV, value).apply()
    }

    fun setExitNumberShowInNav(context: Context, value: Boolean) {
        exitNumberShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_EXIT_NUMBER_NAV, value).apply()
    }

    fun setFuelStationShowInNav(context: Context, value: Boolean) {
        fuelStationShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_FUEL_STATION_NAV, value).apply()
    }

    fun setTransitStopShowInNav(context: Context, value: Boolean) {
        transitStopShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_TRANSIT_STOP_NAV, value).apply()
    }

    fun setPoiPlaceShowInNav(context: Context, value: Boolean) {
        poiPlaceShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_POI_PLACE_NAV, value).apply()
    }

    fun setParkingShowInNav(context: Context, value: Boolean) {
        parkingShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_PARKING_NAV, value).apply()
    }

    fun setSavedPinShowInNav(context: Context, value: Boolean) {
        savedPinShowInNav.value = value
        prefs(context).edit().putBoolean(KEY_SAVED_PIN_NAV, value).apply()
    }

    private fun prefs(c: Context) = c.getSharedPreferences("vela_settings", Context.MODE_PRIVATE)
    private const val KEY_POIS = "map_show_pois"
    private const val KEY_TRANSIT = "map_show_transit_stops"
    private const val KEY_CIVIC = "map_show_civic_pois"
    private const val KEY_SCALE = "map_poi_icon_scale"
    internal const val KEY_SPEEDCAM_SCALE = "map_nav_speedcam_scale"
    internal const val KEY_FLOCK_SCALE = "map_nav_flock_scale"
    internal const val KEY_SURV_CAM_SCALE = "map_nav_surv_cam_scale"
    private const val KEY_SIGNAL_SCALE = "map_nav_signal_scale"
    private const val KEY_STOP_SCALE = "map_nav_stop_scale"
    private const val KEY_RAILX_SCALE = "map_nav_railx_scale"
    private const val KEY_HUMP_SCALE = "map_nav_hump_scale"
    private const val KEY_ROAD_SHIELD_SCALE = "map_icon_road_shield_scale"
    private const val KEY_EXIT_NUMBER_SCALE = "map_icon_exit_number_scale"
    private const val KEY_FUEL_STATION_SCALE = "map_icon_fuel_station_scale"
    private const val KEY_TRANSIT_STOP_SCALE = "map_icon_transit_stop_scale"
    private const val KEY_POI_PLACE_SCALE = "map_icon_poi_place_scale"
    private const val KEY_PARKING_SCALE = "map_icon_parking_scale"
    private const val KEY_SAVED_PIN_SCALE = "map_icon_saved_pin_scale"

    private const val KEY_SPEEDCAM_MIN_ZOOM = "map_nav_speedcam_min_zoom"
    private const val KEY_FLOCK_MIN_ZOOM = "map_nav_flock_min_zoom"
    private const val KEY_SURV_CAM_MIN_ZOOM = "map_nav_surv_cam_min_zoom"
    private const val KEY_SIGNAL_MIN_ZOOM = "map_nav_signal_min_zoom"
    private const val KEY_STOP_MIN_ZOOM = "map_nav_stop_min_zoom"
    private const val KEY_RAILX_MIN_ZOOM = "map_nav_railx_min_zoom"
    private const val KEY_HUMP_MIN_ZOOM = "map_nav_hump_min_zoom"
    private const val KEY_ROAD_SHIELD_MIN_ZOOM = "map_icon_road_shield_min_zoom"
    private const val KEY_EXIT_NUMBER_MIN_ZOOM = "map_icon_exit_number_min_zoom"
    private const val KEY_FUEL_STATION_MIN_ZOOM = "map_icon_fuel_station_min_zoom"
    private const val KEY_TRANSIT_STOP_MIN_ZOOM = "map_icon_transit_stop_min_zoom"
    private const val KEY_POI_PLACE_MIN_ZOOM = "map_icon_poi_place_min_zoom"
    private const val KEY_PARKING_MIN_ZOOM = "map_icon_parking_min_zoom"
    private const val KEY_SAVED_PIN_MIN_ZOOM = "map_icon_saved_pin_min_zoom"

    private const val KEY_SPEEDCAM_NAV = "map_nav_speedcam_show_nav"
    private const val KEY_FLOCK_NAV = "map_nav_flock_show_nav"
    private const val KEY_SURV_CAM_NAV = "map_nav_surv_cam_show_nav"
    private const val KEY_SIGNAL_NAV = "map_nav_signal_show_nav"
    private const val KEY_STOP_NAV = "map_nav_stop_show_nav"
    private const val KEY_RAILX_NAV = "map_nav_railx_show_nav"
    private const val KEY_HUMP_NAV = "map_nav_hump_show_nav"
    private const val KEY_ROAD_SHIELD_NAV = "map_icon_road_shield_show_nav"
    private const val KEY_EXIT_NUMBER_NAV = "map_icon_exit_number_show_nav"
    private const val KEY_FUEL_STATION_NAV = "map_icon_fuel_station_show_nav"
    private const val KEY_TRANSIT_STOP_NAV = "map_icon_transit_stop_show_nav"
    private const val KEY_POI_PLACE_NAV = "map_icon_poi_place_show_nav"
    private const val KEY_PARKING_NAV = "map_icon_parking_show_nav"
    private const val KEY_SAVED_PIN_NAV = "map_icon_saved_pin_show_nav"
    private const val KEY_PLACES_SOURCE = "map_places_source"
    private const val KEY_PLACES_WITH_DOWNLOADS = "offline_places_with_downloads"
    private const val KEY_LOOKUP_TAPPED = "map_places_google_lookup"
    private const val KEY_NAV_TAP_PLACES = "map_places_nav_tap"
    private const val KEY_NAV_TAP_FORCED_POIS = "map_places_nav_tap_forced_pois"
    const val SOURCE_OPEN = "open"
    const val SOURCE_GOOGLE = "google"
    const val SOURCE_BOTH = "both"
}
