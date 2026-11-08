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
            ) { app.vela.ui.MapPoiPrefs.setSpeedcamScale(context, it) },
            PictogramRow(
                R.string.settings_nav_icon_alpr_cam,
                flockBmp,
                app.vela.ui.MapPoiPrefs.flockScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setFlockScale(context, it) },
            PictogramRow(
                R.string.settings_nav_icon_surveillance_cam,
                survBmp,
                app.vela.ui.MapPoiPrefs.surveillanceCamScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setSurveillanceCamScale(context, it) },
            PictogramRow(
                R.string.settings_nav_icon_waze,
                wazeBmp,
                app.vela.ui.WazeAlertsPref.wazeIconScale.floatValue,
            ) { app.vela.ui.WazeAlertsPref.setWazeIconScale(context, it) },
            PictogramRow(
                R.string.settings_nav_icon_signal,
                signalBmp,
                app.vela.ui.MapPoiPrefs.signalScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setSignalScale(context, it) },
            PictogramRow(
                R.string.settings_nav_icon_stop,
                stopBmp,
                app.vela.ui.MapPoiPrefs.stopSignScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setStopSignScale(context, it) },
            PictogramRow(
                R.string.settings_nav_icon_railx,
                railxBmp,
                app.vela.ui.MapPoiPrefs.railCrossingScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setRailCrossingScale(context, it) },
            PictogramRow(
                R.string.settings_nav_icon_hump,
                humpBmp,
                app.vela.ui.MapPoiPrefs.speedHumpScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setSpeedHumpScale(context, it) },
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
        val fuelBmp = remember {
            app.vela.ui.map.PoiIcons.groupMarker(context, "fuel") ?: pinBmp
        }
        val poiBmp = remember {
            app.vela.ui.map.PoiIcons.groupMarker(context, "food") ?: pinBmp
        }

        val mapRows = listOf(
            PictogramRow(
                R.string.settings_map_icon_road_shield,
                shieldBmp,
                app.vela.ui.MapPoiPrefs.roadShieldScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setRoadShieldScale(context, it) },
            PictogramRow(
                R.string.settings_map_icon_exit_number,
                exitBmp,
                app.vela.ui.MapPoiPrefs.exitNumberScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setExitNumberScale(context, it) },
            PictogramRow(
                R.string.settings_map_icon_fuel_station,
                fuelBmp,
                app.vela.ui.MapPoiPrefs.fuelStationScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setFuelStationScale(context, it) },
            PictogramRow(
                R.string.settings_map_icon_transit_stop,
                transitBmp,
                app.vela.ui.MapPoiPrefs.transitStopScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setTransitStopScale(context, it) },
            PictogramRow(
                R.string.settings_map_icon_poi_place,
                poiBmp,
                app.vela.ui.MapPoiPrefs.poiPlaceScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setPoiPlaceScale(context, it) },
            PictogramRow(
                R.string.settings_map_icon_parking,
                parkingBmp,
                app.vela.ui.MapPoiPrefs.parkingScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setParkingScale(context, it) },
            PictogramRow(
                R.string.settings_map_icon_saved_pin,
                pinBmp,
                app.vela.ui.MapPoiPrefs.savedPinScale.floatValue,
            ) { app.vela.ui.MapPoiPrefs.setSavedPinScale(context, it) },
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
