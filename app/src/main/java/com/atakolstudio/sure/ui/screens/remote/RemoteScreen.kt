package com.atakolstudio.sure.ui.screens.remote

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.atakolstudio.sure.data.ir.AcFanSpeed
import com.atakolstudio.sure.data.ir.AcMode
import com.atakolstudio.sure.data.ir.RemoteButton
import com.atakolstudio.sure.domain.model.DeviceType
import com.atakolstudio.sure.ui.components.RemoteIconButton
import com.atakolstudio.sure.ui.components.RemoteTextButton

// --- Küçük renk yardımcıları: tema rengini gradyan için açıklaştır/koyulaştır ---
private fun Color.lighten(fraction: Float): Color = lerp(this, Color.White, fraction)
private fun Color.darken(fraction: Float): Color = lerp(this, Color.Black, fraction)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    onBack: () -> Unit,
    viewModel: RemoteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Kumandayı kullanırken telefon ekranı kararıp kilitlenmesin — gerçek bir
    // uzaktan kumanda kullanırken bu son derece can sıkıcı olurdu.
    KeepScreenOn()

    LaunchedEffect(state.lastMessage) {
        state.lastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.background
        ),
        endY = 900f
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DeviceAvatar(deviceType = state.deviceType)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(state.nickname.ifBlank { "Uzaktan Kumanda" }, fontWeight = FontWeight.Bold)
                            state.brand?.let {
                                Text(
                                    it.displayNameEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            if (!state.hasIrHardware) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .zIndex(1f)
                ) {
                    Text(
                        "Bu cihazda kızılötesi (IR) verici bulunmuyor. Sinyaller gönderilemeyecek.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            when (state.deviceType) {
                DeviceType.AC -> AcRemoteLayout(state = state, viewModel = viewModel)
                else -> TvLikeRemoteLayout(state = state, viewModel = viewModel)
            }
        }
    }
}

/** Başlıkta cihaz türünü temsil eden küçük, renkli, gradyanlı bir rozet/avatar. */
@Composable
private fun DeviceAvatar(deviceType: DeviceType) {
    val icon = when (deviceType) {
        DeviceType.TV -> Icons.Filled.Tv
        DeviceType.AC -> Icons.Filled.AcUnit
        DeviceType.SET_TOP_BOX -> Icons.Filled.SettingsInputHdmi
        DeviceType.AV_RECEIVER -> Icons.Filled.Speaker
        DeviceType.STREAMING_MEDIA -> Icons.Filled.Cast
        DeviceType.HOME_AUTOMATION -> Icons.Filled.Home
        DeviceType.DISC_PLAYER -> Icons.Filled.Album
        DeviceType.PROJECTOR -> Icons.Filled.Videocam
    }
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(primary.lighten(0.25f), primary.darken(0.1f)))),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

// =========================================================================
// KLİMA (AC) — sıcaklık + mod + fan hızı tabanlı, TV'den tamamen farklı arayüz
// =========================================================================

@Composable
private fun AcRemoteLayout(state: RemoteUiState, viewModel: RemoteViewModel) {
    val scrollState = rememberScrollState()
    val coolColor = Color(0xFF2196F3)
    val heatColor = Color(0xFFFF7043)
    val accentColor = when (state.acMode) {
        AcMode.COOL -> coolColor
        AcMode.HEAT -> heatColor
        AcMode.FAN -> MaterialTheme.colorScheme.primary
        AcMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // Durum göstergesi
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (state.acIsOn) accentColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (state.acIsOn) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.acIsOn) "AÇIK" else "KAPALI",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (state.acIsOn) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Sıcaklık göstergesi — gradyanlı, dairesel kart
        Box(
            modifier = Modifier
                .size(220.dp)
                .shadow(16.dp, CircleShape, spotColor = accentColor.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.18f), MaterialTheme.colorScheme.surfaceVariant),
                        radius = 320f
                    )
                )
                .border(2.dp, accentColor.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedContent(
                    targetState = state.acTemperature,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInVertically { h -> h } + fadeIn()) togetherWith (slideOutVertically { h -> -h } + fadeOut())
                        } else {
                            (slideInVertically { h -> -h } + fadeIn()) togetherWith (slideOutVertically { h -> h } + fadeOut())
                        }
                    },
                    label = "acTemperature"
                ) { temp ->
                    Text(
                        "$temp°",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Black,
                        color = accentColor
                    )
                }
                Text(
                    "${viewModel.acTemperatureRange.first}–${viewModel.acTemperatureRange.last}°C aralığı",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            RemoteIconButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Sıcaklığı Azalt",
                onClick = { viewModel.acDecreaseTemperature() },
                modifier = Modifier.shadow(4.dp, CircleShape),
                size = 60.dp,
                repeatable = true
            )
            RemoteIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "Sıcaklığı Artır",
                onClick = { viewModel.acIncreaseTemperature() },
                modifier = Modifier.shadow(4.dp, CircleShape),
                size = 60.dp,
                repeatable = true
            )
        }

        Spacer(Modifier.height(32.dp))

        // Mod seçimi
        Text("MOD", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AcModeChip(label = "Soğutma", icon = Icons.Filled.AcUnit, selected = state.acMode == AcMode.COOL, accentColor = coolColor) { viewModel.acSetMode(AcMode.COOL) }
            AcModeChip(label = "Isıtma", icon = Icons.Filled.WbSunny, selected = state.acMode == AcMode.HEAT, accentColor = heatColor) { viewModel.acSetMode(AcMode.HEAT) }
            AcModeChip(label = "Fan", icon = Icons.Filled.Air, selected = state.acMode == AcMode.FAN, accentColor = MaterialTheme.colorScheme.primary) { viewModel.acSetMode(AcMode.FAN) }
        }

        Spacer(Modifier.height(28.dp))

        // Fan hızı seçimi
        Text("FAN HIZI", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AcFanChip(label = "Düşük", selected = state.acFanSpeed == AcFanSpeed.LOW) { viewModel.acSetFanSpeed(AcFanSpeed.LOW) }
            AcFanChip(label = "Orta", selected = state.acFanSpeed == AcFanSpeed.MED) { viewModel.acSetFanSpeed(AcFanSpeed.MED) }
            AcFanChip(label = "Yüksek", selected = state.acFanSpeed == AcFanSpeed.HIGH) { viewModel.acSetFanSpeed(AcFanSpeed.HIGH) }
        }

        Spacer(Modifier.height(40.dp))

        // Güç (kapat) butonu — gradyanlı
        RemoteIconButton(
            icon = Icons.Filled.PowerSettingsNew,
            contentDescription = "Kapat",
            onClick = { viewModel.acPowerOff() },
            modifier = Modifier.shadow(14.dp, CircleShape, spotColor = MaterialTheme.colorScheme.error),
            size = 72.dp,
            containerBrush = Brush.radialGradient(
                listOf(MaterialTheme.colorScheme.error.lighten(0.15f), MaterialTheme.colorScheme.error.darken(0.1f))
            ),
            iconTint = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text("Kapat", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        Spacer(Modifier.height(20.dp))

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp)) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Bu jenerik/örnek klima profili, yaygın bir OEM klima modülünün gerçek " +
                        "kodlarını kullanır. Cihazınız tepki vermezse, marka-özel klima desteği " +
                        "henüz eklenmemiş olabilir.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcModeChip(label: String, icon: ImageVector, selected: Boolean, accentColor: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor.copy(alpha = 0.18f),
            selectedLabelColor = accentColor,
            selectedLeadingIconColor = accentColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcFanChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

// =========================================================================
// TV / Set-üstü kutu / AV Alıcısı / Ortam Yayıncısı / Disk Oynatıcı / Projektör
// / Ev Otomasyonu — ortak D-pad + ses/kanal tabanlı arayüz
// =========================================================================

@Composable
private fun TvLikeRemoteLayout(state: RemoteUiState, viewModel: RemoteViewModel) {
    var numpadExpanded by remember { mutableStateOf(false) }
    var extraExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val mappedButtons = state.brand?.commands?.keys ?: emptySet()
    val extraButtons = listOf(
        RemoteButton.SETTINGS to "Ayarlar",
        RemoteButton.PLAY_PAUSE to "Oynat/Duraklat",
        RemoteButton.STOP to "Durdur",
        RemoteButton.REWIND to "Geri Sar",
        RemoteButton.FAST_FORWARD to "İleri Sar"
    ).filter { it.first in mappedButtons }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val errorColor = MaterialTheme.colorScheme.error
        RemoteIconButton(
            icon = Icons.Filled.PowerSettingsNew,
            contentDescription = "Güç",
            onClick = { viewModel.sendCommand(RemoteButton.POWER) },
            modifier = Modifier.shadow(16.dp, CircleShape, spotColor = errorColor.copy(alpha = 0.7f)),
            size = 78.dp,
            containerBrush = Brush.radialGradient(listOf(errorColor.lighten(0.15f), errorColor.darken(0.12f))),
            iconTint = Color.White
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            TintedIconButton(Icons.Filled.Menu, "Menü", MaterialTheme.colorScheme.primary) { viewModel.sendCommand(RemoteButton.MENU) }
            TintedIconButton(Icons.Filled.Home, "Akıllı Ana Sayfa", MaterialTheme.colorScheme.primary) { viewModel.sendCommand(RemoteButton.HOME) }
            TintedIconButton(Icons.Filled.Input, "Giriş", MaterialTheme.colorScheme.primary) { viewModel.sendCommand(RemoteButton.INPUT) }
        }

        Spacer(Modifier.height(28.dp))

        DPad(
            onUp = { viewModel.sendCommand(RemoteButton.UP) },
            onDown = { viewModel.sendCommand(RemoteButton.DOWN) },
            onLeft = { viewModel.sendCommand(RemoteButton.LEFT) },
            onRight = { viewModel.sendCommand(RemoteButton.RIGHT) },
            onOk = { viewModel.sendCommand(RemoteButton.OK) }
        )

        Spacer(Modifier.height(14.dp))

        RemoteIconButton(
            icon = Icons.Filled.ArrowBackIosNew,
            contentDescription = "Çıkış",
            onClick = { viewModel.sendCommand(RemoteButton.BACK) },
            modifier = Modifier.shadow(3.dp, CircleShape)
        )

        Spacer(Modifier.height(28.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            VerticalRocker(
                label = "SES",
                onUp = { viewModel.sendCommand(RemoteButton.VOLUME_UP) },
                onDown = { viewModel.sendCommand(RemoteButton.VOLUME_DOWN) },
                middleIcon = Icons.Filled.VolumeOff,
                onMiddleClick = { viewModel.sendCommand(RemoteButton.MUTE) }
            )
            VerticalRocker(
                label = "KANAL",
                onUp = { viewModel.sendCommand(RemoteButton.CHANNEL_UP) },
                onDown = { viewModel.sendCommand(RemoteButton.CHANNEL_DOWN) }
            )
        }

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            ColorKey(Color(0xFFE53935)) { viewModel.sendCommand(RemoteButton.RED) }
            ColorKey(Color(0xFF43A047)) { viewModel.sendCommand(RemoteButton.GREEN) }
            ColorKey(Color(0xFFFDD835)) { viewModel.sendCommand(RemoteButton.YELLOW) }
            ColorKey(Color(0xFF1E88E5)) { viewModel.sendCommand(RemoteButton.BLUE) }
        }

        if (extraButtons.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = { extraExpanded = !extraExpanded }) {
                Text(if (extraExpanded) "Diğer Fonksiyonları Gizle" else "Diğer Fonksiyonlar")
                Icon(
                    imageVector = if (extraExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
            AnimatedVisibility(visible = extraExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                    extraButtons.forEach { (button, label) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RemoteIconButton(
                                icon = iconForExtraButton(button),
                                contentDescription = label,
                                onClick = { viewModel.sendCommand(button) },
                                size = 48.dp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        TextButton(onClick = { numpadExpanded = !numpadExpanded }) {
            Text(if (numpadExpanded) "Sayısal Tuş Takımını Gizle" else "Sayısal Tuş Takımını Göster")
            Icon(
                imageVector = if (numpadExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
        }

        AnimatedVisibility(visible = numpadExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            NumPad(onDigit = { digit -> viewModel.sendCommand(digitToButton(digit)) })
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TintedIconButton(icon: ImageVector, contentDescription: String, tint: Color, onClick: () -> Unit) {
    RemoteIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        backgroundColor = tint.copy(alpha = 0.12f),
        iconTint = tint
    )
}

private fun iconForExtraButton(button: RemoteButton): ImageVector = when (button) {
    RemoteButton.SETTINGS -> Icons.Filled.Settings
    RemoteButton.PLAY_PAUSE -> Icons.Filled.PlayArrow
    RemoteButton.STOP -> Icons.Filled.Stop
    RemoteButton.REWIND -> Icons.Filled.FastRewind
    RemoteButton.FAST_FORWARD -> Icons.Filled.FastForward
    else -> Icons.Filled.Circle
}

private fun digitToButton(digit: Int): RemoteButton = when (digit) {
    0 -> RemoteButton.NUM_0; 1 -> RemoteButton.NUM_1; 2 -> RemoteButton.NUM_2
    3 -> RemoteButton.NUM_3; 4 -> RemoteButton.NUM_4; 5 -> RemoteButton.NUM_5
    6 -> RemoteButton.NUM_6; 7 -> RemoteButton.NUM_7; 8 -> RemoteButton.NUM_8
    else -> RemoteButton.NUM_9
}

@Composable
private fun DPad(
    onUp: () -> Unit, onDown: () -> Unit, onLeft: () -> Unit, onRight: () -> Unit, onOk: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(228.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(228.dp)
                .shadow(10.dp, CircleShape, spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.lighten(0.4f),
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), CircleShape)
        )
        RemoteIconButton(Icons.Filled.KeyboardArrowUp, "Yukarı", onUp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp), backgroundColor = Color.Transparent)
        RemoteIconButton(Icons.Filled.KeyboardArrowDown, "Aşağı", onDown, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), backgroundColor = Color.Transparent)
        RemoteIconButton(Icons.Filled.KeyboardArrowLeft, "Sol", onLeft, modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp), backgroundColor = Color.Transparent)
        RemoteIconButton(Icons.Filled.KeyboardArrowRight, "Sağ", onRight, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp), backgroundColor = Color.Transparent)
        RemoteIconButton(
            icon = Icons.Filled.Check,
            contentDescription = "Tamam",
            onClick = onOk,
            modifier = Modifier.shadow(8.dp, CircleShape, spotColor = primary.copy(alpha = 0.6f)),
            size = 70.dp,
            containerBrush = Brush.radialGradient(listOf(primary.lighten(0.15f), primary.darken(0.08f))),
            iconTint = Color.White
        )
    }
}

@Composable
private fun VerticalRocker(
    label: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    middleIcon: ImageVector? = null,
    onMiddleClick: (() -> Unit)? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(28.dp), spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            RemoteIconButton(Icons.Filled.Add, "$label Artır", onUp, backgroundColor = Color.Transparent, repeatable = true)
            if (middleIcon != null && onMiddleClick != null) {
                RemoteIconButton(middleIcon, "$label Sessiz", onMiddleClick, backgroundColor = Color.Transparent, size = 44.dp)
            } else {
                Spacer(Modifier.height(8.dp))
            }
            RemoteIconButton(Icons.Filled.Remove, "$label Azalt", onDown, backgroundColor = Color.Transparent, repeatable = true)
        }
    }
}

@Composable
private fun ColorKey(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .shadow(4.dp, CircleShape, spotColor = color.copy(alpha = 0.6f))
            .clip(CircleShape)
            .background(Color.White)
            .padding(2.dp)
    ) {
        RemoteIconButton(
            icon = Icons.Filled.Circle,
            contentDescription = "Renkli Tuş",
            onClick = onClick,
            size = 34.dp,
            backgroundColor = color,
            iconTint = color
        )
    }
}

@Composable
private fun NumPad(onDigit: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.height(220.dp).padding(top = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items((1..9).toList()) { digit ->
            RemoteTextButton(text = digit.toString(), onClick = { onDigit(digit) }, size = 60.dp)
        }
        item { Spacer(Modifier) }
        item { RemoteTextButton(text = "0", onClick = { onDigit(0) }, size = 60.dp) }
    }
}

/** Kumandayı kullanırken telefon ekranının kararıp kilitlenmesini engeller. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}
