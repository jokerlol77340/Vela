package app.vela.ui.settings.sections

import app.vela.ui.icons.Sym

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vela.R
import app.vela.ui.dpadHighlight
import app.vela.ui.dpadRowSibling
import app.vela.ui.settings.GroupDivider
import app.vela.ui.settings.settingsAnchor
import app.vela.ui.settings.Hint
import app.vela.ui.settings.SelectableRow
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.ToggleRow

// Settings groups shared across screens after the 2026-09-17 reshuffle.

/** Surveillance and speed cameras, avoidance, and live Waze alerts: the map markers and every alert about them */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun CameraSettingsGroup() {
    val context = LocalContext.current

    // 1. Surveillance and ALPR cameras
    SettingsGroup(title = stringResource(R.string.settings_cameras_title)) {
        ToggleRow(
            label = stringResource(R.string.settings_flock_alpr_display),
            checked = app.vela.ui.Flock.showAlpr.value,
            onCheckedChange = { app.vela.ui.Flock.setShowAlpr(context, it) },
            hint = stringResource(R.string.settings_flock_alpr_hint),
            leading = { app.vela.ui.PlateCameraBadge() },
        )
        ToggleRow(
            label = stringResource(R.string.settings_flock_surveillance_display),
            checked = app.vela.ui.Flock.showSurveillance.value,
            onCheckedChange = { app.vela.ui.Flock.setShowSurveillance(context, it) },
            hint = stringResource(R.string.settings_flock_surveillance_hint),
            leading = { app.vela.ui.SurveillanceCameraBadge() },
        )
        ToggleRow(
            label = stringResource(R.string.settings_flock_nav_card),
            checked = app.vela.ui.FlockNavAlert.card.value,
            onCheckedChange = { app.vela.ui.FlockNavAlert.setCard(context, it) },
            hint = stringResource(R.string.settings_flock_nav_card_hint),
        )
        ToggleRow(
            label = stringResource(R.string.settings_flock_nav_voice),
            checked = app.vela.ui.FlockNavAlert.voice.value,
            onCheckedChange = { app.vela.ui.FlockNavAlert.setVoice(context, it) },
            hint = stringResource(R.string.settings_flock_nav_voice_hint),
        )
        if (app.vela.ui.FlockNavAlert.any) {
            CameraDistanceSelector(
                selectedMeters = app.vela.ui.FlockNavAlert.distanceMeters.intValue,
                onSelect = { app.vela.ui.FlockNavAlert.setDistanceMeters(context, it) },
            )
        }
    }

    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

    // 2. Speed cameras (Radars)
    SettingsGroup(title = stringResource(R.string.settings_speed_cams)) {
        ToggleRow(
            label = stringResource(R.string.settings_speed_cams_display),
            checked = app.vela.ui.SpeedCams.on.value,
            onCheckedChange = { app.vela.ui.SpeedCams.set(context, it) },
            hint = stringResource(R.string.settings_speed_cams_hint),
            leading = { app.vela.ui.SpeedCameraBadge() },
        )
        ToggleRow(
            label = stringResource(R.string.settings_speed_cam_nav_card),
            checked = app.vela.ui.SpeedCamWarn.card.value,
            onCheckedChange = { app.vela.ui.SpeedCamWarn.setCard(context, it) },
            hint = stringResource(R.string.settings_speed_cam_nav_card_hint),
        )
        ToggleRow(
            label = stringResource(R.string.settings_speed_cam_nav_voice),
            checked = app.vela.ui.SpeedCamWarn.voice.value,
            onCheckedChange = { app.vela.ui.SpeedCamWarn.setVoice(context, it) },
            hint = stringResource(R.string.settings_speed_cam_nav_voice_hint),
        )
        if (app.vela.ui.SpeedCamWarn.any) {
            CameraDistanceSelector(
                selectedMeters = app.vela.ui.SpeedCamWarn.distanceMeters.intValue,
                onSelect = { app.vela.ui.SpeedCamWarn.setDistanceMeters(context, it) },
            )
        }
    }

    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

    // 3. Multi-choice camera & radar avoidance section
    SettingsGroup(title = stringResource(R.string.settings_camera_avoidance_group_title)) {
        androidx.compose.foundation.layout.Column(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Hint(stringResource(R.string.settings_camera_avoidance_group_hint))
            androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
            CameraAvoidModeMultiSelector(
                modeLabel = stringResource(R.string.settings_flock_mode_drive),
                mode = app.vela.core.model.TravelMode.DRIVE,
                selectedTargets = app.vela.ui.FlockRouteAlert.driveTargets.value,
                onToggleTarget = {
                    app.vela.ui.FlockRouteAlert.toggleTarget(context, app.vela.core.model.TravelMode.DRIVE, it)
                    app.vela.ui.RouteActions.camerasChanged?.invoke()
                },
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            CameraAvoidModeMultiSelector(
                modeLabel = stringResource(R.string.settings_flock_mode_walk),
                mode = app.vela.core.model.TravelMode.WALK,
                selectedTargets = app.vela.ui.FlockRouteAlert.walkTargets.value,
                onToggleTarget = {
                    app.vela.ui.FlockRouteAlert.toggleTarget(context, app.vela.core.model.TravelMode.WALK, it)
                    app.vela.ui.RouteActions.camerasChanged?.invoke()
                },
            )
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
            CameraAvoidModeMultiSelector(
                modeLabel = stringResource(R.string.settings_flock_mode_bike),
                mode = app.vela.core.model.TravelMode.BICYCLE,
                selectedTargets = app.vela.ui.FlockRouteAlert.bikeTargets.value,
                onToggleTarget = {
                    app.vela.ui.FlockRouteAlert.toggleTarget(context, app.vela.core.model.TravelMode.BICYCLE, it)
                    app.vela.ui.RouteActions.camerasChanged?.invoke()
                },
            )
        }
        if (app.vela.ui.FlockRouteAlert.on.value) {
            GroupDivider()
            val curMode = app.vela.ui.FlockDetour.mode.value
            SelectableRow(
                label = stringResource(R.string.settings_flock_detour_disabled),
                selected = curMode == app.vela.ui.CameraAvoidDetourMode.DISABLED,
                onClick = {
                    app.vela.ui.FlockDetour.setMode(context, app.vela.ui.CameraAvoidDetourMode.DISABLED)
                    app.vela.ui.RouteActions.camerasChanged?.invoke()
                },
                hint = stringResource(R.string.settings_flock_detour_disabled_hint),
            )
            SelectableRow(
                label = app.vela.ui.FlockDetour.dynamicModerateLabel(context),
                selected = curMode == app.vela.ui.CameraAvoidDetourMode.MODERATE,
                onClick = {
                    app.vela.ui.FlockDetour.setMode(context, app.vela.ui.CameraAvoidDetourMode.MODERATE)
                    app.vela.ui.RouteActions.camerasChanged?.invoke()
                },
                hint = stringResource(R.string.settings_flock_detour_moderate_hint),
            )
            SelectableRow(
                label = stringResource(R.string.settings_flock_detour_total),
                selected = curMode == app.vela.ui.CameraAvoidDetourMode.TOTAL_AVOIDANCE,
                onClick = {
                    app.vela.ui.FlockDetour.setMode(context, app.vela.ui.CameraAvoidDetourMode.TOTAL_AVOIDANCE)
                    app.vela.ui.RouteActions.camerasChanged?.invoke()
                },
                hint = stringResource(R.string.settings_flock_detour_total_hint),
            )
        }
    }

    androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))

    // 4. Live Waze alerts along active route
    SettingsGroup(title = stringResource(R.string.settings_waze_alerts_title)) {
        ToggleRow(
            label = stringResource(R.string.settings_waze_alerts_enable),
            checked = app.vela.ui.WazeAlertsPref.enabled.value,
            onCheckedChange = {
                app.vela.ui.WazeAlertsPref.setEnabled(context, it)
                app.vela.ui.RouteActions.wazeChanged?.invoke()
            },
            hint = stringResource(R.string.settings_waze_alerts_hint),
            leading = { app.vela.ui.WazePoliceBadge() },
        )
        if (app.vela.ui.WazeAlertsPref.enabled.value) {
            androidx.compose.foundation.layout.Column(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = app.vela.ui.WazeAlertsPref.showPolice.value,
                        onClick = {
                            app.vela.ui.WazeAlertsPref.setShowPolice(context, !app.vela.ui.WazeAlertsPref.showPolice.value)
                        },
                        label = { Text(stringResource(R.string.settings_waze_police)) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
                    )
                    FilterChip(
                        selected = app.vela.ui.WazeAlertsPref.showHazards.value,
                        onClick = {
                            app.vela.ui.WazeAlertsPref.setShowHazards(context, !app.vela.ui.WazeAlertsPref.showHazards.value)
                        },
                        label = { Text(stringResource(R.string.settings_waze_hazards)) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
                    )
                    FilterChip(
                        selected = app.vela.ui.WazeAlertsPref.showAccidents.value,
                        onClick = {
                            app.vela.ui.WazeAlertsPref.setShowAccidents(context, !app.vela.ui.WazeAlertsPref.showAccidents.value)
                        },
                        label = { Text(stringResource(R.string.settings_waze_accidents)) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
                    )
                }
            }
            ToggleRow(
                label = stringResource(R.string.settings_waze_nav_card),
                checked = app.vela.ui.WazeAlertsPref.cardWarn.value,
                onCheckedChange = { app.vela.ui.WazeAlertsPref.setCardWarn(context, it) },
                hint = stringResource(R.string.settings_waze_nav_card_hint),
            )
            ToggleRow(
                label = stringResource(R.string.settings_waze_nav_voice),
                checked = app.vela.ui.WazeAlertsPref.voiceWarn.value,
                onCheckedChange = { app.vela.ui.WazeAlertsPref.setVoiceWarn(context, it) },
                hint = stringResource(R.string.settings_waze_nav_voice_hint),
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CameraDistanceSelector(
    selectedMeters: Int,
    onSelect: (Int) -> Unit,
) {
    val distances = listOf(
        0 to R.string.settings_speed_cam_dist_auto,
        200 to R.string.settings_speed_cam_dist_200,
        400 to R.string.settings_speed_cam_dist_400,
        600 to R.string.settings_speed_cam_dist_600,
        800 to R.string.settings_speed_cam_dist_800,
        1000 to R.string.settings_speed_cam_dist_1000,
    )
    androidx.compose.foundation.layout.Column(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            stringResource(R.string.settings_speed_cam_distance_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val distFocus = remember { List(distances.size) { FocusRequester() } }
            distances.forEachIndexed { i, (meters, labelRes) ->
                FilterChip(
                    selected = selectedMeters == meters,
                    onClick = { onSelect(meters) },
                    label = { Text(stringResource(labelRes)) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier
                        .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                        .dpadRowSibling(distFocus, i),
                )
            }
        }
        Hint(stringResource(R.string.settings_speed_cam_distance_hint))
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CameraAvoidModeMultiSelector(
    modeLabel: String,
    mode: app.vela.core.model.TravelMode,
    selectedTargets: Set<app.vela.ui.CameraAvoidTarget>,
    onToggleTarget: (app.vela.ui.CameraAvoidTarget) -> Unit,
) {
    val allowed = app.vela.ui.CameraAvoidTarget.allowedFor(mode)
    androidx.compose.foundation.layout.Column {
        Text(
            modeLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val focus = remember { List(allowed.size) { FocusRequester() } }
            allowed.forEachIndexed { i, target ->
                val isSelected = target in selectedTargets
                val labelRes = when (target) {
                    app.vela.ui.CameraAvoidTarget.ALPR -> R.string.camera_avoid_alpr
                    app.vela.ui.CameraAvoidTarget.SURVEILLANCE -> R.string.camera_avoid_surveillance
                    app.vela.ui.CameraAvoidTarget.RADAR -> R.string.camera_avoid_radar
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleTarget(target) },
                    label = { Text(stringResource(labelRes)) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier
                        .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                        .dpadRowSibling(focus, i),
                )
            }
        }
    }
}

/** What the map draws for places: the master switch, tapped-place lookup, civic places,
 *  transit stops and icon size (moved from Map to Places, 2026-09-17). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun PlacesOnMapGroup() {
    val context = LocalContext.current
        SettingsGroup(title = stringResource(R.string.settings_map_places)) {
        ToggleRow(
            label = stringResource(R.string.settings_show_pois),
            checked = app.vela.ui.MapPoiPrefs.showPois.value,
            onCheckedChange = { app.vela.ui.MapPoiPrefs.setShowPois(context, it) },
            hint = stringResource(R.string.settings_show_pois_hint),
        )
        if (app.vela.ui.MapPoiPrefs.showPois.value) {
            if (app.vela.ui.MapPoiPrefs.openPlaces) {
                GroupDivider()
                TappedLookupRow()
            }
            GroupDivider()
            ToggleRow(
                label = stringResource(R.string.settings_show_civic),
                checked = app.vela.ui.MapPoiPrefs.showCivic.value,
                onCheckedChange = { app.vela.ui.MapPoiPrefs.setShowCivic(context, it) },
                hint = stringResource(R.string.settings_show_civic_hint),
            )
        }
        GroupDivider()
        ToggleRow(
            label = stringResource(R.string.settings_show_transit_stops),
            checked = app.vela.ui.MapPoiPrefs.showTransit.value,
            onCheckedChange = { app.vela.ui.MapPoiPrefs.setShowTransit(context, it) },
            hint = stringResource(R.string.settings_show_transit_stops_hint),
        )
        // Which kinds of stop (discussion #648). Shown only while stops are on.
        if (app.vela.ui.MapPoiPrefs.showTransit.value) {
            ToggleRow(
                label = stringResource(R.string.settings_transit_stops_bus),
                checked = app.vela.ui.TransitLayer.stopBus.value,
                onCheckedChange = { app.vela.ui.TransitLayer.setStopBus(context, it) },
            )
            ToggleRow(
                label = stringResource(R.string.settings_transit_stops_metro),
                checked = app.vela.ui.TransitLayer.stopMetro.value,
                onCheckedChange = { app.vela.ui.TransitLayer.setStopMetro(context, it) },
            )
            ToggleRow(
                label = stringResource(R.string.settings_transit_stops_train),
                checked = app.vela.ui.TransitLayer.stopTrain.value,
                onCheckedChange = { app.vela.ui.TransitLayer.setStopTrain(context, it) },
            )
        }
        GroupDivider()
        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.settings_poi_icon_size),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier.settingsAnchor(stringResource(R.string.settings_poi_icon_size)).padding(top = 4.dp),
            )
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val sizeFocus = remember { List(3) { FocusRequester() } }
                listOf(
                    R.string.settings_poi_size_small to 0.7f,
                    R.string.settings_poi_size_default to 1.0f,
                    R.string.settings_poi_size_large to 1.25f,
                ).forEachIndexed { i, (label, value) ->
                    FilterChip(
                        selected = kotlin.math.abs(app.vela.ui.MapPoiPrefs.iconScale.floatValue - value) < 0.01f,
                        onClick = { app.vela.ui.MapPoiPrefs.setIconScale(context, value) },
                        label = { Text(stringResource(label)) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                            .dpadRowSibling(sizeFocus, i),
                    )
                }
            }
            Hint(stringResource(R.string.settings_poi_icon_size_hint))
        }
        }
}

/** Where the map's places come from: Vela data, Google, or both, with what each costs. */
@Composable
internal fun PlacesSourceGroup(topRow: Modifier = Modifier, compact: Boolean = false) {
    val context = LocalContext.current
        SettingsGroup {
            // Where the map's businesses come from (Map, then Privacy on 2026-09-16, then Places on
            // 2026-09-17 so everything about places sits together; the hints still spell out what
            // leaves the phone as you pan). Each option states its own cost so the choice
            // is the user's: open data is offline and quiet, Google is complete and chatty, both
            // is the open layer plus one Google fetch per settled view.
            androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    stringResource(R.string.settings_places_source),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            listOf(
                app.vela.ui.MapPoiPrefs.SOURCE_OPEN to R.string.settings_places_source_open,
                app.vela.ui.MapPoiPrefs.SOURCE_GOOGLE to R.string.settings_places_source_google,
                app.vela.ui.MapPoiPrefs.SOURCE_BOTH to R.string.settings_places_source_both,
            ).forEachIndexed { i, (id, label) ->
                SelectableRow(
                    modifier = if (i == 0) topRow else Modifier,
                    label = stringResource(label),
                    selected = app.vela.ui.MapPoiPrefs.placesSource.value == id,
                    onClick = { app.vela.ui.MapPoiPrefs.setPlacesSource(context, id) },
                )
            }
            Hint(
                stringResource(
                    when (app.vela.ui.MapPoiPrefs.placesSource.value) {
                        app.vela.ui.MapPoiPrefs.SOURCE_GOOGLE -> R.string.settings_places_source_google_hint
                        app.vela.ui.MapPoiPrefs.SOURCE_BOTH -> R.string.settings_places_source_both_hint
                        else -> R.string.settings_places_source_open_hint
                    },
                ),
            )
            // The one-list overview keeps the picker and its one-line cost; the rest is on Places.
            if (!compact) {
                // Said once, for all three: the choice is about the map's icons, nothing else.
                Hint(stringResource(R.string.settings_places_source_note))
                // The short hints carry what matters; the rest (who maintains the data, where Vela
                // serves it from, what still touches Google) lives behind Learn more.
                var placesInfo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                androidx.compose.material3.TextButton(
                    onClick = { placesInfo = true },
                    modifier = Modifier.padding(start = 8.dp).dpadHighlight(androidx.compose.foundation.shape.CircleShape),
                ) { Text(stringResource(R.string.settings_places_source_more)) }
                if (placesInfo) {
                    app.vela.ui.VelaDialog(
                        onDismissRequest = { placesInfo = false },
                        title = stringResource(R.string.settings_places_source_more_title),
                        text = { Text(stringResource(R.string.settings_places_source_more_body)) },
                        confirmText = stringResource(android.R.string.ok),
                        onConfirm = { placesInfo = false },
                        dismissText = stringResource(R.string.settings_places_source_more_credit),
                        onDismiss = {
                            placesInfo = false
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://overturemaps.org/")),
                                )
                            }
                        },
                        dismissLowEmphasis = true,
                    )
                }
            }
            // ("OpenStreetMap shops too" is gone, 2026-09-23: the places bake carries OSM's
            // businesses and landmarks itself now, and the basemap's own point layers are hidden
            // over every archive baked with them. An older archive still gets OSM's shops drawn
            // under it, deduped by name, the switch's old default.)
        }
}

/** The ~2 min traffic and route re-check during navigation (a Google request each time). */
@Composable
internal fun LiveRechecksGroup(vm: app.vela.ui.map.MapViewModel) {
        SettingsGroup { LiveRechecksRow(vm) }
}

/** Simulated drive and simulated location, for demos and screenshots (moved from Navigation to
 *  Diagnostics, 2026-09-17: they are test tools, and leaving one on breaks real navigation). */
@Composable
internal fun DemoModesGroup(vm: app.vela.ui.map.MapViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("vela_settings", android.content.Context.MODE_PRIVATE) }
        var demoDrive by remember { mutableStateOf(prefs.getBoolean("demo_drive", false)) }
        SettingsGroup {
        ToggleRow(
            label = stringResource(R.string.settings_demo_drive),
            checked = demoDrive,
            onCheckedChange = {
                demoDrive = it
                prefs.edit().putBoolean("demo_drive", it).apply()
            },
            hint = stringResource(R.string.settings_demo_drive_hint),
        )

        // Simulated location - pretend to be at the current map center (for demos / screenshots
        // without leaking where you actually are). Reactive holder so the switch reflects state.
        GroupDivider()
        ToggleRow(
            label = stringResource(R.string.settings_sim_location),
            checked = app.vela.ui.SimLocation.on,
            onCheckedChange = { on -> if (on) vm.simulateLocationHere() else vm.stopSimulateLocation() },
            hint = stringResource(R.string.settings_sim_location_hint),
        )
        }
}

/** Recent "parked here" saves, so an accidental overwrite is recoverable (moved from Navigation
 *  to Saved places, 2026-09-17). */
@Composable
internal fun ParkingHistoryGroup(vm: app.vela.ui.map.MapViewModel) {
        // Parking history - recent "parked here" saves, so an accidental overwrite is
        // recoverable (also reachable by long-pressing the P button on the map).
        // Always present (issue #426): the settings search lists "Parking history", and a group
        // that only existed once you had parked led the match to nothing on a fresh install.
        val state by vm.state.collectAsStateWithLifecycle()
        run {
            Spacer(Modifier.height(8.dp))
            SettingsGroup(title = stringResource(R.string.settings_parking_history)) {
            if (state.parkingHistory.isEmpty()) {
                Hint(stringResource(R.string.settings_parking_history_empty))
            } else {
            Hint(stringResource(R.string.settings_parking_history_hint))
            Box(Modifier.padding(horizontal = 8.dp)) {
                TextButton(onClick = { vm.clearParkingHistory() }) { Text(stringResource(R.string.parking_history_clear_all)) }
            }
            }
            state.parkingHistory.forEachIndexed { pi, entry ->
                if (pi > 0) GroupDivider()
                val isCurrent = entry.savedAtMillis == state.parkedAtMillis
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Sym.LocalParking,
                        contentDescription = null,
                        tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        app.vela.ui.formatDateTime(androidx.compose.ui.platform.LocalContext.current, entry.savedAtMillis),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCurrent) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCurrent) {
                        Text(stringResource(R.string.parking_history_current), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    } else {
                        // D-pad: Restore/Delete sit side by side inside the L/R-swallowing Column,
                        // so the pair drives its own LEFT/RIGHT (issue #24 pattern).
                        val rowFocus = remember(entry.savedAtMillis) { List(2) { FocusRequester() } }
                        TextButton(modifier = Modifier.dpadRowSibling(rowFocus, 0), onClick = { vm.restoreParkingFromHistory(entry) }) { Text(stringResource(R.string.parking_history_restore)) }
                        IconButton(modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape).dpadRowSibling(rowFocus, 1), onClick = { vm.deleteParkingHistoryEntry(entry) }) {
                            Icon(Sym.Delete, contentDescription = stringResource(R.string.parking_history_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }
        }
}
