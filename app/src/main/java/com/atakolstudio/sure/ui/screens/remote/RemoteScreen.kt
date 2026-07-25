package com.atakolstudio.sure.ui.screens.remote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(
    onBack: () -> Unit,
    viewModel: RemoteViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.lastMessage) {
        state.lastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.nickname.ifBlank { "Uzaktan Kumanda" }, fontWeight = FontWeight.SemiBold)
                        state.brand?.let {
                            Text(it.displayNameEn, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(Modifier.padding(padding).fillMaxSize()) {
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

// =========================================================================
// KLİMA (AC) — sıcaklık + mod + fan hızı tabanlı, TV'den tamamen farklı arayüz
// =========================================================================

@Composable
private fun AcRemoteLayout(state: RemoteUiState, viewModel: RemoteViewModel) {
    val scrollState = rememberScrollState()

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
            color = if (state.acIsOn) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                if (state.acIsOn) "AÇIK" else "KAPALI",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (state.acIsOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(24.dp))

        // Sıcaklık göstergesi + step'leri
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            RemoteIconButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Sıcaklığı Azalt",
                onClick = { viewModel.acDecreaseTemperature() },
                size = 64.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${state.acTemperature}°",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${viewModel.acTemperatureRange.first}–${viewModel.acTemperatureRange.last}°C aralığı",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            RemoteIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "Sıcaklığı Artır",
                onClick = { viewModel.acIncreaseTemperature() },
                size = 64.dp
            )
        }

        Spacer(Modifier.height(32.dp))

        // Mod seçimi
        Text("MOD", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AcModeChip(label = "Soğutma", icon = Icons.Filled.AcUnit, selected = state.acMode == AcMode.COOL) { viewModel.acSetMode(AcMode.COOL) }
            AcModeChip(label = "Isıtma", icon = Icons.Filled.WbSunny, selected = state.acMode == AcMode.HEAT) { viewModel.acSetMode(AcMode.HEAT) }
            AcModeChip(label = "Fan", icon = Icons.Filled.Air, selected = state.acMode == AcMode.FAN) { viewModel.acSetMode(AcMode.FAN) }
        }

        Spacer(Modifier.height(28.dp))

        // Fan hızı seçimi
        Text("FAN HIZI", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AcFanChip(label = "Düşük", selected = state.acFanSpeed == AcFanSpeed.LOW) { viewModel.acSetFanSpeed(AcFanSpeed.LOW) }
            AcFanChip(label = "Orta", selected = state.acFanSpeed == AcFanSpeed.MED) { viewModel.acSetFanSpeed(AcFanSpeed.MED) }
            AcFanChip(label = "Yüksek", selected = state.acFanSpeed == AcFanSpeed.HIGH) { viewModel.acSetFanSpeed(AcFanSpeed.HIGH) }
        }

        Spacer(Modifier.height(40.dp))

        // Güç (kapat) butonu
        RemoteIconButton(
            icon = Icons.Filled.PowerSettingsNew,
            contentDescription = "Kapat",
            onClick = { viewModel.acPowerOff() },
            size = 72.dp,
            backgroundColor = MaterialTheme.colorScheme.error,
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
            Text(
                "Bu jenerik/örnek klima profili, yaygın bir OEM klima modülünün gerçek " +
                    "kodlarını kullanır. Cihazınız tepki vermezse, marka-özel klima desteği " +
                    "henüz eklenmemiş olabilir.",
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AcModeChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
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
        RemoteIconButton(
            icon = Icons.Filled.PowerSettingsNew,
            contentDescription = "Güç",
            onClick = { viewModel.sendCommand(RemoteButton.POWER) },
            size = 76.dp,
            backgroundColor = MaterialTheme.colorScheme.error,
            iconTint = Color.White
        )

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            RemoteIconButton(Icons.Filled.Menu, "Menü", { viewModel.sendCommand(RemoteButton.MENU) })
            RemoteIconButton(Icons.Filled.Home, "Akıllı Ana Sayfa", { viewModel.sendCommand(RemoteButton.HOME) })
            RemoteIconButton(Icons.Filled.Input, "Giriş", { viewModel.sendCommand(RemoteButton.INPUT) })
        }

        Spacer(Modifier.height(24.dp))

        DPad(
            onUp = { viewModel.sendCommand(RemoteButton.UP) },
            onDown = { viewModel.sendCommand(RemoteButton.DOWN) },
            onLeft = { viewModel.sendCommand(RemoteButton.LEFT) },
            onRight = { viewModel.sendCommand(RemoteButton.RIGHT) },
            onOk = { viewModel.sendCommand(RemoteButton.OK) }
        )

        Spacer(Modifier.height(12.dp))

        RemoteIconButton(
            icon = Icons.Filled.ArrowBackIosNew,
            contentDescription = "Çıkış",
            onClick = { viewModel.sendCommand(RemoteButton.BACK) }
        )

        Spacer(Modifier.height(24.dp))

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

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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

private fun iconForExtraButton(button: RemoteButton): androidx.compose.ui.graphics.vector.ImageVector = when (button) {
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
    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        RemoteIconButton(Icons.Filled.KeyboardArrowUp, "Yukarı", onUp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp), backgroundColor = Color.Transparent)
        RemoteIconButton(Icons.Filled.KeyboardArrowDown, "Aşağı", onDown, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp), backgroundColor = Color.Transparent)
        RemoteIconButton(Icons.Filled.KeyboardArrowLeft, "Sol", onLeft, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp), backgroundColor = Color.Transparent)
        RemoteIconButton(Icons.Filled.KeyboardArrowRight, "Sağ", onRight, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), backgroundColor = Color.Transparent)
        RemoteIconButton(
            icon = Icons.Filled.Check,
            contentDescription = "Tamam",
            onClick = onOk,
            size = 68.dp,
            backgroundColor = MaterialTheme.colorScheme.primary,
            iconTint = Color.White
        )
    }
}

@Composable
private fun VerticalRocker(
    label: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    middleIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onMiddleClick: (() -> Unit)? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            RemoteIconButton(Icons.Filled.Add, "$label Artır", onUp, backgroundColor = Color.Transparent)
            if (middleIcon != null && onMiddleClick != null) {
                RemoteIconButton(middleIcon, "$label Sessiz", onMiddleClick, backgroundColor = Color.Transparent, size = 44.dp)
            } else {
                Spacer(Modifier.height(8.dp))
            }
            RemoteIconButton(Icons.Filled.Remove, "$label Azalt", onDown, backgroundColor = Color.Transparent)
        }
    }
}

@Composable
private fun ColorKey(color: Color, onClick: () -> Unit) {
    RemoteIconButton(
        icon = Icons.Filled.Circle,
        contentDescription = "Renkli Tuş",
        onClick = onClick,
        size = 36.dp,
        backgroundColor = color,
        iconTint = color
    )
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
