package app.vela.ui.settings.sections

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vela.R
import app.vela.ui.Units
import app.vela.ui.dpadHighlight
import app.vela.ui.dpadRowSibling
import app.vela.ui.map.MapViewModel
import app.vela.ui.settings.GroupDivider
import app.vela.ui.settings.Hint
import app.vela.ui.settings.SelectableRow
import app.vela.ui.settings.SettingsGroup
import app.vela.ui.settings.SettingsScaffold
import app.vela.ui.settings.SubHead
import app.vela.ui.settings.ToggleRow
import app.vela.ui.theme.AppTheme
import app.vela.ui.theme.ThemeMode
import kotlinx.coroutines.launch

private data class PictogramRow(
    val labelRes: Int,
    val bitmap: android.graphics.Bitmap,
    val currentScale: Float,
    val onScaleChange: (Float) -> Unit,
    val currentMinZoom: Float = 0f,
    val onMinZoomChange: (Float) -> Unit = {},
    val showInMap: Boolean = true,
    val onShowInMapChange: (Boolean) -> Unit = {},
    val showInNav: Boolean = true,
    val onShowInNavChange: (Boolean) -> Unit = {},
)

/** Appearance sub-screen divided into 3 sub-tabs (Theme & Map, Pictograms, Text & Units). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSettingsScreen(vm: MapViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.settings_appearance_tab_theme,
        R.string.settings_appearance_tab_icons,
        R.string.settings_appearance_tab_text,
    )

    SettingsScaffold(stringResource(R.string.settings_appearance), onBack) { topRow ->
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        ) {
            tabs.forEachIndexed { index, titleRes ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            stringResource(titleRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        when (selectedTab) {
            0 -> AppearanceThemeTab(context, topRow)
            1 -> AppearanceIconsTab(context)
            2 -> AppearanceTextAndUnitsTab(context)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AppearanceThemeTab(context: Context, topRow: Modifier) {
    SettingsGroup {
        SelectableRow(
            label = stringResource(R.string.settings_follow_system),
            selected = AppTheme.mode.value == ThemeMode.SYSTEM,
            onClick = { AppTheme.set(context, ThemeMode.SYSTEM) },
            modifier = topRow,
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_theme_light),
            selected = AppTheme.mode.value == ThemeMode.LIGHT,
            onClick = { AppTheme.set(context, ThemeMode.LIGHT) },
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_theme_dark),
            selected = AppTheme.mode.value == ThemeMode.DARK,
            onClick = { AppTheme.set(context, ThemeMode.DARK) },
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_theme_amoled),
            selected = AppTheme.mode.value == ThemeMode.AMOLED,
            onClick = { AppTheme.set(context, ThemeMode.AMOLED) },
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_theme_auto),
            selected = AppTheme.mode.value == ThemeMode.AUTO,
            onClick = { AppTheme.set(context, ThemeMode.AUTO) },
        )
        if (AppTheme.mode.value == ThemeMode.SYSTEM || AppTheme.mode.value == ThemeMode.AUTO) {
            GroupDivider()
            ToggleRow(
                label = stringResource(R.string.settings_theme_amoled_for_dark),
                checked = AppTheme.amoledDark.value,
                onCheckedChange = { AppTheme.setAmoledDark(context, it) },
                hint = stringResource(R.string.settings_theme_amoled_for_dark_hint),
            )
        }
    }
    Hint(stringResource(R.string.settings_theme_auto_hint))
    Hint(stringResource(R.string.settings_appearance_hint))

    Spacer(Modifier.height(8.dp))
    SubHead(stringResource(R.string.settings_map_theme_title))
    SettingsGroup {
        SelectableRow(
            label = stringResource(R.string.settings_map_theme_follow),
            selected = AppTheme.mapMode.value == app.vela.ui.theme.MapThemeMode.FOLLOW,
            onClick = { AppTheme.setMap(context, app.vela.ui.theme.MapThemeMode.FOLLOW) },
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_theme_light),
            selected = AppTheme.mapMode.value == app.vela.ui.theme.MapThemeMode.LIGHT,
            onClick = { AppTheme.setMap(context, app.vela.ui.theme.MapThemeMode.LIGHT) },
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_theme_dark),
            selected = AppTheme.mapMode.value == app.vela.ui.theme.MapThemeMode.DARK,
            onClick = { AppTheme.setMap(context, app.vela.ui.theme.MapThemeMode.DARK) },
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_theme_amoled),
            selected = AppTheme.mapMode.value == app.vela.ui.theme.MapThemeMode.AMOLED,
            onClick = { AppTheme.setMap(context, app.vela.ui.theme.MapThemeMode.AMOLED) },
        )
        if (AppTheme.mapMode.value == app.vela.ui.theme.MapThemeMode.FOLLOW) {
            GroupDivider()
            ToggleRow(
                label = stringResource(R.string.settings_map_theme_amoled_for_dark),
                checked = AppTheme.mapAmoledDark.value,
                onCheckedChange = { AppTheme.setMapAmoledDark(context, it) },
                hint = stringResource(R.string.settings_map_theme_amoled_for_dark_hint),
            )
        }
    }
    Hint(stringResource(R.string.settings_map_theme_hint))

    if (AppTheme.mode.value != ThemeMode.AUTO) {
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            ToggleRow(
                label = stringResource(R.string.settings_nav_day_night),
                checked = AppTheme.navDayNight.value,
                onCheckedChange = { AppTheme.setNavDayNight(context, it) },
                hint = stringResource(R.string.settings_nav_day_night_hint),
            )
            if (AppTheme.navDayNight.value && AppTheme.mode.value != ThemeMode.SYSTEM) {
                GroupDivider()
                ToggleRow(
                    label = stringResource(R.string.settings_theme_amoled_for_dark),
                    checked = AppTheme.amoledDark.value,
                    onCheckedChange = { AppTheme.setAmoledDark(context, it) },
                    hint = stringResource(R.string.settings_theme_amoled_for_nav_hint),
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    SettingsGroup(title = stringResource(R.string.settings_map_look)) {
        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val looks = app.vela.core.data.tiles.MapStyle.entries
                val lookFocus = remember { List(looks.size) { FocusRequester() } }
                looks.forEachIndexed { i, look ->
                    val label = when (look) {
                        app.vela.core.data.tiles.MapStyle.LIBERTY -> R.string.settings_map_look_liberty
                        app.vela.core.data.tiles.MapStyle.BRIGHT -> R.string.settings_map_look_bright
                        app.vela.core.data.tiles.MapStyle.POSITRON -> R.string.settings_map_look_positron
                        app.vela.core.data.tiles.MapStyle.FIORD -> R.string.settings_map_look_fiord
                        app.vela.core.data.tiles.MapStyle.DARK -> R.string.settings_map_look_dark
                    }
                    FilterChip(
                        selected = app.vela.ui.MapLook.style.value == look,
                        onClick = { app.vela.ui.MapLook.set(context, look) },
                        label = { Text(stringResource(label)) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                            .dpadRowSibling(lookFocus, i),
                    )
                }
            }
            Hint(stringResource(R.string.settings_map_look_hint))
        }
    }

    if (app.vela.ui.MapLook.style.value.painted) {
        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_map_colors)) {
            androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val palFocus = remember { List(3) { FocusRequester() } }
                    listOf(
                        app.vela.ui.MapColors.MODERN to stringResource(R.string.settings_map_colors_modern),
                        app.vela.ui.MapColors.CLASSIC to stringResource(R.string.settings_map_colors_classic),
                        app.vela.ui.MapColors.CONTRAST to stringResource(R.string.settings_map_colors_contrast),
                    ).forEachIndexed { i, (id, label) ->
                        FilterChip(
                            selected = app.vela.ui.MapColors.current() == id,
                            onClick = { app.vela.ui.MapColors.set(context, id) },
                            label = { Text(label) },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier
                                .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                                .dpadRowSibling(palFocus, i),
                        )
                    }
                }
                Hint(stringResource(R.string.settings_map_colors_hint))
            }
        }
        Spacer(Modifier.height(8.dp))
        SettingsGroup(title = stringResource(R.string.settings_map_buttons)) {
            androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val shapeFocus = remember { List(2) { FocusRequester() } }
                    listOf(
                        app.vela.ui.MapButtonShape.ROUND to stringResource(R.string.settings_map_buttons_round),
                        app.vela.ui.MapButtonShape.SQUARE to stringResource(R.string.settings_map_buttons_square),
                    ).forEachIndexed { i, (id, label) ->
                        FilterChip(
                            selected = app.vela.ui.MapButtonShape.mode.value == id,
                            onClick = { app.vela.ui.MapButtonShape.set(context, id) },
                            label = { Text(label) },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier
                                .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                                .dpadRowSibling(shapeFocus, i),
                        )
                    }
                }
                Hint(stringResource(R.string.settings_map_buttons_hint))
            }
        }
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        Spacer(Modifier.height(8.dp))
        SettingsGroup {
            ToggleRow(
                label = stringResource(R.string.settings_dynamic_color),
                checked = app.vela.ui.theme.DynamicColor.on.value,
                onCheckedChange = { app.vela.ui.theme.DynamicColor.set(context, it) },
                hint = stringResource(R.string.settings_dynamic_color_hint),
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AppearanceIconsTab(context: Context) {
    SettingsGroup(title = stringResource(R.string.settings_ui_scale)) {
        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val scaleFocus = remember { List(4) { FocusRequester() } }
                listOf(0.9f, 1.0f, 1.15f, 1.3f).forEachIndexed { i, f ->
                    FilterChip(
                        selected = kotlin.math.abs(app.vela.ui.UiScale.factor.value - f) < 0.01f,
                        onClick = { app.vela.ui.UiScale.set(context, f) },
                        label = { Text("${(f * 100).toInt()}%") },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                            .dpadRowSibling(scaleFocus, i),
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    SettingsGroup(title = stringResource(R.string.settings_nav_icon_scale)) {
        val speedcamBmp = remember { app.vela.ui.map.speedCamBitmap() }
        val flockBmp = remember { app.vela.ui.map.alprCameraBitmap() }
        val survBmp = remember { app.vela.ui.map.surveillanceCameraBitmap() }
        val wazeBmp = remember { app.vela.ui.map.wazePoliceBitmap() }
        val signalBmp = remember { app.vela.ui.map.trafficLightBitmap() }
        val stopBmp = remember { app.vela.ui.map.stopSignBitmap() }
        val railxBmp = remember { app.vela.ui.map.railCrossingBitmap() }
        val humpBmp = remember { app.vela.ui.map.speedHumpBitmap() }

        val navRows = listOf(
            PictogramRow(
                R.string.settings_nav_icon_speedcam,
                speedcamBmp,
                app.vela.ui.MapPoiPrefs.speedcamScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setSpeedcamScale(context, it) },
                app.vela.ui.MapPoiPrefs.speedcamMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setSpeedcamMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.speedcamShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setSpeedcamShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.speedcamShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setSpeedcamShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_nav_icon_alpr_cam,
                flockBmp,
                app.vela.ui.MapPoiPrefs.flockScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setFlockScale(context, it) },
                app.vela.ui.MapPoiPrefs.flockMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setFlockMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.flockShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setFlockShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.flockShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setFlockShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_nav_icon_surveillance_cam,
                survBmp,
                app.vela.ui.MapPoiPrefs.surveillanceCamScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setSurveillanceCamScale(context, it) },
                app.vela.ui.MapPoiPrefs.surveillanceCamMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setSurveillanceCamMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.surveillanceCamShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setSurveillanceCamShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.surveillanceCamShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setSurveillanceCamShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_nav_icon_waze,
                wazeBmp,
                app.vela.ui.WazeAlertsPref.wazeIconScale.floatValue,
                { app.vela.ui.WazeAlertsPref.setWazeIconScale(context, it) },
                app.vela.ui.WazeAlertsPref.wazeMinZoom.floatValue,
                { app.vela.ui.WazeAlertsPref.setWazeMinZoom(context, it) },
                app.vela.ui.WazeAlertsPref.wazeShowInMap.value,
                { app.vela.ui.WazeAlertsPref.setWazeShowInMap(context, it) },
                app.vela.ui.WazeAlertsPref.wazeShowInNav.value,
                { app.vela.ui.WazeAlertsPref.setWazeShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_nav_icon_signal,
                signalBmp,
                app.vela.ui.MapPoiPrefs.signalScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setSignalScale(context, it) },
                app.vela.ui.MapPoiPrefs.signalMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setSignalMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.signalShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setSignalShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.signalShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setSignalShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_nav_icon_stop,
                stopBmp,
                app.vela.ui.MapPoiPrefs.stopSignScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setStopSignScale(context, it) },
                app.vela.ui.MapPoiPrefs.stopSignMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setStopSignMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.stopSignShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setStopSignShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.stopSignShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setStopSignShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_nav_icon_railx,
                railxBmp,
                app.vela.ui.MapPoiPrefs.railCrossingScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setRailCrossingScale(context, it) },
                app.vela.ui.MapPoiPrefs.railCrossingMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setRailCrossingMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.railCrossingShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setRailCrossingShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.railCrossingShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setRailCrossingShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_nav_icon_hump,
                humpBmp,
                app.vela.ui.MapPoiPrefs.speedHumpScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setSpeedHumpScale(context, it) },
                app.vela.ui.MapPoiPrefs.speedHumpMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setSpeedHumpMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.speedHumpShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setSpeedHumpShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.speedHumpShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setSpeedHumpShowInNav(context, it) },
            ),
        )

        val uniformScales = listOf(0f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f)
        PictogramRowsList(
            rows = navRows,
            scales = uniformScales,
        )
        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
            Hint(stringResource(R.string.settings_nav_icon_scale_hint))
        }
    }

    Spacer(Modifier.height(8.dp))
    SettingsGroup(title = stringResource(R.string.settings_map_icon_scale)) {
        val shieldBmp = remember { app.vela.ui.map.RoadShields.previewShieldBitmap() }
        val exitBmp = remember { app.vela.ui.map.RoadShields.previewExitBitmap() }
        val transitBmp = remember { app.vela.ui.map.transitStopBitmap() }
        val parkingBmp = remember { app.vela.ui.map.parkingBitmap() }
        val pinBmp = remember { app.vela.ui.map.pinBitmap() }
        val fuelBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "fuel") ?: pinBmp }
        val foodBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "food") ?: pinBmp }
        val shopBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "shop") ?: pinBmp }
        val sportBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "sport") ?: pinBmp }
        val eduBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "edu") ?: pinBmp }
        val healthBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "health") ?: pinBmp }
        val lodgingBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "lodging") ?: pinBmp }
        val cultureBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "culture") ?: pinBmp }
        val civicBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "civic") ?: pinBmp }
        val parkBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "park") ?: pinBmp }
        val poiBmp = remember { app.vela.ui.map.PoiIcons.groupMarker(context, "default") ?: pinBmp }

        val mapRows = listOf(
            PictogramRow(
                R.string.settings_map_icon_road_shield,
                shieldBmp,
                app.vela.ui.MapPoiPrefs.roadShieldScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setRoadShieldScale(context, it) },
                app.vela.ui.MapPoiPrefs.roadShieldMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setRoadShieldMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.roadShieldShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setRoadShieldShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.roadShieldShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setRoadShieldShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_exit_number,
                exitBmp,
                app.vela.ui.MapPoiPrefs.exitNumberScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setExitNumberScale(context, it) },
                app.vela.ui.MapPoiPrefs.exitNumberMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setExitNumberMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.exitNumberShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setExitNumberShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.exitNumberShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setExitNumberShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_fuel_station,
                fuelBmp,
                app.vela.ui.MapPoiPrefs.fuelStationScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setFuelStationScale(context, it) },
                app.vela.ui.MapPoiPrefs.fuelStationMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setFuelStationMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.fuelStationShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setFuelStationShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.fuelStationShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setFuelStationShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_parking,
                parkingBmp,
                app.vela.ui.MapPoiPrefs.parkingScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setParkingScale(context, it) },
                app.vela.ui.MapPoiPrefs.parkingMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setParkingMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.parkingShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setParkingShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.parkingShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setParkingShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_food,
                foodBmp,
                app.vela.ui.MapPoiPrefs.foodScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setFoodScale(context, it) },
                app.vela.ui.MapPoiPrefs.foodMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setFoodMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.foodShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setFoodShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.foodShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setFoodShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_shop,
                shopBmp,
                app.vela.ui.MapPoiPrefs.shopScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setShopScale(context, it) },
                app.vela.ui.MapPoiPrefs.shopMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setShopMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.shopShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setShopShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.shopShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setShopShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_sport,
                sportBmp,
                app.vela.ui.MapPoiPrefs.sportScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setSportScale(context, it) },
                app.vela.ui.MapPoiPrefs.sportMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setSportMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.sportShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setSportShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.sportShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setSportShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_edu,
                eduBmp,
                app.vela.ui.MapPoiPrefs.eduScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setEduScale(context, it) },
                app.vela.ui.MapPoiPrefs.eduMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setEduMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.eduShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setEduShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.eduShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setEduShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_health,
                healthBmp,
                app.vela.ui.MapPoiPrefs.healthScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setHealthScale(context, it) },
                app.vela.ui.MapPoiPrefs.healthMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setHealthMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.healthShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setHealthShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.healthShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setHealthShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_lodging,
                lodgingBmp,
                app.vela.ui.MapPoiPrefs.lodgingScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setLodgingScale(context, it) },
                app.vela.ui.MapPoiPrefs.lodgingMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setLodgingMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.lodgingShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setLodgingShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.lodgingShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setLodgingShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_culture,
                cultureBmp,
                app.vela.ui.MapPoiPrefs.cultureScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setCultureScale(context, it) },
                app.vela.ui.MapPoiPrefs.cultureMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setCultureMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.cultureShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setCultureShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.cultureShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setCultureShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_civic,
                civicBmp,
                app.vela.ui.MapPoiPrefs.civicScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setCivicScale(context, it) },
                app.vela.ui.MapPoiPrefs.civicMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setCivicMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.civicShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setCivicShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.civicShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setCivicShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_park,
                parkBmp,
                app.vela.ui.MapPoiPrefs.parkScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setParkScale(context, it) },
                app.vela.ui.MapPoiPrefs.parkMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setParkMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.parkShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setParkShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.parkShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setParkShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_transit_stop,
                transitBmp,
                app.vela.ui.MapPoiPrefs.transitStopScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setTransitStopScale(context, it) },
                app.vela.ui.MapPoiPrefs.transitStopMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setTransitStopMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.transitStopShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setTransitStopShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.transitStopShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setTransitStopShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_poi_place,
                poiBmp,
                app.vela.ui.MapPoiPrefs.poiPlaceScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setPoiPlaceScale(context, it) },
                app.vela.ui.MapPoiPrefs.poiPlaceMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setPoiPlaceMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.poiPlaceShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setPoiPlaceShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.poiPlaceShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setPoiPlaceShowInNav(context, it) },
            ),
            PictogramRow(
                R.string.settings_map_icon_saved_pin,
                pinBmp,
                app.vela.ui.MapPoiPrefs.savedPinScale.floatValue,
                { app.vela.ui.MapPoiPrefs.setSavedPinScale(context, it) },
                app.vela.ui.MapPoiPrefs.savedPinMinZoom.floatValue,
                { app.vela.ui.MapPoiPrefs.setSavedPinMinZoom(context, it) },
                app.vela.ui.MapPoiPrefs.savedPinShowInMap.value,
                { app.vela.ui.MapPoiPrefs.setSavedPinShowInMap(context, it) },
                app.vela.ui.MapPoiPrefs.savedPinShowInNav.value,
                { app.vela.ui.MapPoiPrefs.setSavedPinShowInNav(context, it) },
            ),
        )

        val uniformScales = listOf(0f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.25f, 2.5f)
        PictogramRowsList(
            rows = mapRows,
            scales = uniformScales,
        )
        androidx.compose.foundation.layout.Column(Modifier.padding(horizontal = 16.dp)) {
            Hint(stringResource(R.string.settings_map_icon_scale_hint))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PictogramRowsList(
    rows: List<PictogramRow>,
    scales: List<Float>,
) {
    val zoomLevels = listOf(0f, 0.01f, 8f, 10f, 11f, 12f, 13f, 14f, 15f, 16f)
    rows.forEachIndexed { rowIdx, row ->
        if (rowIdx > 0) GroupDivider()
        androidx.compose.foundation.layout.Column(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 4.dp),
            ) {
                val img = remember(row.bitmap) { row.bitmap.asImageBitmap() }
                val isHidden = row.currentScale <= 0f
                val previewDp = if (isHidden) 22.dp else (22f * row.currentScale).dp
                androidx.compose.foundation.Image(
                    bitmap = img,
                    contentDescription = stringResource(row.labelRes),
                    modifier = Modifier.size(previewDp),
                    alpha = if (isHidden) 0.35f else 1.0f,
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                )
                Text(
                    text = stringResource(row.labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                )
            }
            // 1. Sizing chips
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val navIconFocus = remember { List(scales.size) { FocusRequester() } }
                scales.forEachIndexed { i, f ->
                    FilterChip(
                        selected = kotlin.math.abs(row.currentScale - f) < 0.01f,
                        onClick = { row.onScaleChange(f) },
                        label = { Text(if (f <= 0f) stringResource(R.string.pictogram_hidden) else "${(f * 100).toInt()}%") },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier
                            .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                            .dpadRowSibling(navIconFocus, i),
                    )
                }
            }
            if (row.currentScale > 0f) {
                // 2. Minimum zoom chips
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                ) {
                    Text(
                        stringResource(R.string.pictogram_min_zoom),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val zoomFocus = remember { List(zoomLevels.size) { FocusRequester() } }
                    zoomLevels.forEachIndexed { i, z ->
                        val isSelected = when {
                            z <= 0f -> row.currentMinZoom <= 0f
                            z <= 0.05f -> row.currentMinZoom in 0.001f..0.05f
                            else -> kotlin.math.abs(row.currentMinZoom - z) < 0.1f
                        }
                        FilterChip(
                            selected = isSelected,
                            onClick = { row.onMinZoomChange(z) },
                            label = {
                                Text(
                                    when {
                                        z <= 0f -> stringResource(R.string.pictogram_zoom_default)
                                        z <= 0.05f -> "z0"
                                        else -> "z${z.toInt()}"
                                    }
                                )
                            },
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier
                                .dpadHighlight(androidx.compose.foundation.shape.CircleShape)
                                .dpadRowSibling(zoomFocus, i),
                        )
                    }
                }
                // 3. Display mode chips (Carte & Nav as independent toggles)
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                ) {
                    Text(
                        stringResource(R.string.pictogram_display_mode),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = row.showInMap,
                        onClick = { row.onShowInMapChange(!row.showInMap) },
                        label = { Text(stringResource(R.string.pictogram_mode_map)) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
                    )
                    FilterChip(
                        selected = row.showInNav,
                        onClick = { row.onShowInNavChange(!row.showInNav) },
                        label = { Text(stringResource(R.string.pictogram_mode_nav)) },
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.dpadHighlight(androidx.compose.foundation.shape.CircleShape),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceTextAndUnitsTab(context: Context) {
    SettingsGroup {
        ToggleRow(
            label = stringResource(R.string.settings_page_transitions),
            checked = app.vela.ui.PageTransitions.enabled.value,
            onCheckedChange = { app.vela.ui.PageTransitions.set(context, it) },
            hint = stringResource(R.string.settings_page_transitions_hint),
        )
    }

    Spacer(Modifier.height(8.dp))
    val fontBad = stringResource(R.string.settings_font_bad)
    val fontScope = androidx.compose.runtime.rememberCoroutineScope()
    val fontPicker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            fontScope.launch {
                val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val name = queryDisplayName(context, uri) ?: context.getString(R.string.settings_font_custom)
                    app.vela.ui.AppFont.setCustom(context, uri, name)
                }
                if (!ok) android.widget.Toast.makeText(context, fontBad, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    SettingsGroup(title = stringResource(R.string.settings_font)) {
        SelectableRow(
            label = "Google Sans Flex",
            selected = app.vela.ui.AppFont.builtin.value,
            onClick = { app.vela.ui.AppFont.setBuiltin(context) },
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_font_system),
            selected = app.vela.ui.AppFont.customName.value == null && !app.vela.ui.AppFont.builtin.value,
            onClick = { app.vela.ui.AppFont.clear(context) },
        )
        GroupDivider()
        SelectableRow(
            label = app.vela.ui.AppFont.customName.value ?: stringResource(R.string.settings_font_choose),
            selected = app.vela.ui.AppFont.customName.value != null,
            onClick = { fontPicker.launch(arrayOf("*/*")) },
        )
    }
    Hint(stringResource(R.string.settings_font_flex_hint))

    Spacer(Modifier.height(8.dp))
    SettingsGroup(title = stringResource(R.string.settings_app_name_title)) {
        val generic = remember {
            androidx.compose.runtime.mutableStateOf(app.vela.ui.AppNameAlias.isGeneric(context))
        }
        ToggleRow(
            label = stringResource(R.string.settings_app_name_generic),
            checked = generic.value,
            onCheckedChange = {
                generic.value = it
                app.vela.ui.AppNameAlias.setGeneric(context, it)
            },
            hint = stringResource(R.string.settings_app_name_generic_hint),
        )
    }

    Spacer(Modifier.height(8.dp))
    SettingsGroup(title = stringResource(R.string.settings_units)) {
        SelectableRow(
            label = stringResource(R.string.settings_units_imperial),
            selected = Units.imperial.value,
            onClick = { Units.set(context, true) },
        )
        GroupDivider()
        SelectableRow(
            label = stringResource(R.string.settings_units_metric),
            selected = !Units.imperial.value,
            onClick = { Units.set(context, false) },
        )
    }

    Spacer(Modifier.height(8.dp))
    SettingsGroup(title = stringResource(R.string.settings_clock)) {
        listOf("system" to R.string.settings_clock_system, "12" to R.string.settings_clock_12, "24" to R.string.settings_clock_24).forEachIndexed { i, (key, label) ->
            if (i > 0) GroupDivider()
            SelectableRow(
                label = stringResource(label),
                selected = app.vela.ui.Clock24.mode.value == key,
                onClick = { app.vela.ui.Clock24.set(context, key) },
            )
        }
    }

    Spacer(Modifier.height(8.dp))
    SettingsGroup(title = stringResource(R.string.settings_language)) {
        val followSystemLang = app.vela.ui.AppLocale.language.value.isBlank()
        ToggleRow(
            label = stringResource(R.string.settings_follow_system_language),
            checked = followSystemLang,
            onCheckedChange = { on ->
                app.vela.ui.AppLocale.set(context, if (on) "" else app.vela.ui.AppLocale.deviceDefaultSupported())
            },
        )
        if (!followSystemLang) {
            app.vela.ui.AppLocale.SUPPORTED.forEach { code ->
                GroupDivider()
                SelectableRow(
                    label = app.vela.ui.AppLocale.endonym(code),
                    selected = app.vela.ui.AppLocale.language.value == code,
                    onClick = { app.vela.ui.AppLocale.set(context, code) },
                )
            }
        }
    }
    Hint(stringResource(R.string.settings_language_hint))

    Spacer(Modifier.height(8.dp))
    SettingsGroup(title = stringResource(R.string.settings_directions_language)) {
        val sameAsApp = app.vela.ui.AppLocale.directions.value.isBlank()
        ToggleRow(
            label = stringResource(R.string.settings_directions_same_as_app),
            checked = sameAsApp,
            onCheckedChange = { on ->
                val current = app.vela.ui.AppLocale.language.value.ifBlank { app.vela.ui.AppLocale.deviceDefaultSupported() }
                app.vela.ui.AppLocale.setDirections(context, if (on) "" else current)
            },
        )
        if (!sameAsApp) {
            app.vela.ui.AppLocale.SUPPORTED.forEach { code ->
                GroupDivider()
                SelectableRow(
                    label = app.vela.ui.AppLocale.endonym(code),
                    selected = app.vela.ui.AppLocale.directions.value == code,
                    onClick = { app.vela.ui.AppLocale.setDirections(context, code) },
                )
            }
        }
    }
    Hint(stringResource(R.string.settings_directions_language_hint))
}

/** The human file name behind a document [uri], for the settings row. */
private fun queryDisplayName(context: android.content.Context, uri: android.net.Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
}.getOrNull()
