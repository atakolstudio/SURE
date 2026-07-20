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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.atakolstudio.sure.data.ir.RemoteButton
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
    var numpadExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

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

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!state.hasIrHardware) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Text(
                        "Bu cihazda kızılötesi (IR) verici bulunmuyor. Sinyaller gönderilemeyecek.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Güç butonu
            RemoteIconButton(
                icon = Icons.Filled.PowerSettingsNew,
                contentDescription = "Güç",
                onClick = { viewModel.sendCommand(RemoteButton.POWER) },
                size = 76.dp,
                backgroundColor = MaterialTheme.colorScheme.error,
                iconTint = Color.White
            )

            Spacer(Modifier.height(20.dp))

            // Menü / Giriş / Ana Sayfa satırı
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                RemoteIconButton(Icons.Filled.Menu, "Menü", { viewModel.sendCommand(RemoteButton.MENU) })
                RemoteIconButton(Icons.Filled.Home, "Akıllı Ana Sayfa", { viewModel.sendCommand(RemoteButton.HOME) })
                RemoteIconButton(Icons.Filled.Input, "Giriş", { viewModel.sendCommand(RemoteButton.INPUT) })
            }

            Spacer(Modifier.height(24.dp))

            // D-Pad
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

            // Ses ve Kanal kontrolü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
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

            // Renkli tuşlar
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ColorKey(Color(0xFFE53935)) { viewModel.sendCommand(RemoteButton.RED) }
                ColorKey(Color(0xFF43A047)) { viewModel.sendCommand(RemoteButton.GREEN) }
                ColorKey(Color(0xFFFDD835)) { viewModel.sendCommand(RemoteButton.YELLOW) }
                ColorKey(Color(0xFF1E88E5)) { viewModel.sendCommand(RemoteButton.BLUE) }
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
