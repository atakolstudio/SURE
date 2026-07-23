package com.atakolstudio.sure.ui.screens.manualsearch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.atakolstudio.sure.data.ir.IrProtocol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualSearchScreen(
    onBack: () -> Unit,
    onDeviceSaved: (Long) -> Unit,
    viewModel: ManualSearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.savedDeviceId) {
        state.savedDeviceId?.let { onDeviceSaved(it) }
    }

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
                title = { Text("Manuel Bul", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = if (state.mode == ManualSearchMode.SCAN) 0 else 1) {
                Tab(
                    selected = state.mode == ManualSearchMode.SCAN,
                    onClick = { viewModel.setMode(ManualSearchMode.SCAN) },
                    text = { Text("Kod Tarama") }
                )
                Tab(
                    selected = state.mode == ManualSearchMode.RAW_ENTRY,
                    onClick = { viewModel.setMode(ManualSearchMode.RAW_ENTRY) },
                    text = { Text("Elle Kod Gir") }
                )
            }

            if (!state.hasIrHardware) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        "Bu cihazda IR verici bulunmuyor; test sinyalleri gönderilemeyecek.",
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            when (state.mode) {
                ManualSearchMode.SCAN -> ScanModeContent(state, viewModel)
                ManualSearchMode.RAW_ENTRY -> RawEntryModeContent(state, viewModel)
            }
        }
    }
}

/** Kör Tarama / Aşırı Tarama seçeneklerinin sunulduğu ayrı, özel "Tarama Modu" menüsü. */
@Composable
private fun ScanTierDialog(
    currentTier: ScanTier,
    onSelect: (ScanTier) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tarama Modu") },
        text = {
            Column {
                ScanTier.entries.forEach { tier ->
                    val selected = tier == currentTier
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .selectable(selected = selected, onClick = { onSelect(tier) })
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            RadioButton(selected = selected, onClick = { onSelect(tier) })
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(tier.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    tier.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@Composable
private fun ScanTierChip(tier: ScanTier, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text("Tarama Modu: ${tier.title}") },
        leadingIcon = { Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
}

@Composable
private fun ScanModeContent(state: ManualSearchUiState, viewModel: ManualSearchViewModel) {
    var nickname by remember { mutableStateOf("") }
    var scanTierMenuVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Kumandayı (telefonu) cihazınıza yöneltin, TEST butonuna basın. Cihaz tepki " +
                "verirse (açılır/kapanır) \"Evet, Bu Doğru\" seçin; vermezse sıradakini deneyin.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )

        Spacer(Modifier.height(14.dp))

        ScanTierChip(tier = state.scanTier, onClick = { scanTierMenuVisible = true })

        Spacer(Modifier.height(20.dp))

        if (state.exhausted) {
            EmptyResultState(state = state, onRestart = { viewModel.restartScan() })
        } else {
            val candidate = state.currentCandidate

            Text(
                "Deneme ${state.currentIndex + 1} / ${state.candidates.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (state.currentIndex + 1).toFloat() / state.candidates.size.toFloat() },
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
            )
            Spacer(Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(vertical = 28.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        candidate?.displayNameEn ?: "-",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        candidate?.displayNameLocal ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(visible = state.isAutoScanning) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { viewModel.stopAutoScan() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("DURDUR — Otomatik Tarama Sürüyor", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Cihazınız tepki verir vermez yukarıdaki butona basıp durdurun, ardından \"Evet, Bu Doğru\" ile onaylayın.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }

            AnimatedVisibility(visible = !state.isAutoScanning) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { viewModel.toggleAutoScan() },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("▶ Otomatik Tara")
                    }
                    Button(
                        onClick = { viewModel.testCurrentCandidate() },
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("TEST ET", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text("Cihazınız tepki verdi mi?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("Cihaz için isim (opsiyonel)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.nextCandidate() },
                    modifier = Modifier.weight(1f)
                ) { Text("Hayır, Sıradaki") }

                Button(
                    onClick = { viewModel.confirmCurrentMatch(nickname) },
                    modifier = Modifier.weight(1f)
                ) { Text("Evet, Bu Doğru") }
            }

            if (state.currentIndex > 0) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { viewModel.previousCandidate() }) { Text("← Öncekine Dön") }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (scanTierMenuVisible) {
        ScanTierDialog(
            currentTier = state.scanTier,
            onSelect = {
                viewModel.setScanTier(it)
                scanTierMenuVisible = false
            },
            onDismiss = { scanTierMenuVisible = false }
        )
    }
}

@Composable
private fun EmptyResultState(state: ManualSearchUiState, onRestart: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(12.dp))
        Text("Listedeki hiçbir kod eşleşmedi.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            when (state.scanTier) {
                ScanTier.EXTREME -> "Aşırı Tarama dahil tüm kodlar denendi. \"Elle Kod Gir\" sekmesini deneyebilirsiniz."
                ScanTier.BLIND -> "Kör Tarama dahil tüm kodlar denendi. \"Tarama Modu\"ndan Aşırı Tarama'yı deneyebilir, ya da \"Elle Kod Gir\" sekmesine geçebilirsiniz."
                ScanTier.STANDARD -> "Sağ üstteki ayar simgesinden \"Kör Tarama\" moduna geçerek gerçek LIRC verileriyle arayabilirsiniz."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRestart) { Text("Baştan Başla") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RawEntryModeContent(state: ManualSearchUiState, viewModel: ManualSearchViewModel) {
    var nickname by remember { mutableStateOf("") }
    var protocolMenuExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp)
    ) {
        Text(
            "Cihazınızın markası listede yok mu veya kod taraması sonuç vermedi mi? " +
                "IR alıcılı bir uygulamayla orijinal kumandanızın gönderdiği protokol, " +
                "adres ve komut değerlerini okuyup buraya girebilirsiniz.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
        )
        Spacer(Modifier.height(20.dp))

        ExposedDropdownMenuBox(
            expanded = protocolMenuExpanded,
            onExpandedChange = { protocolMenuExpanded = it }
        ) {
            OutlinedTextField(
                value = state.rawProtocol.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Protokol") },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = protocolMenuExpanded,
                onDismissRequest = { protocolMenuExpanded = false }
            ) {
                IrProtocol.entries.forEach { protocol ->
                    DropdownMenuItem(
                        text = { Text(protocol.name) },
                        onClick = {
                            viewModel.setRawProtocol(protocol)
                            protocolMenuExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.rawAddressText,
            onValueChange = viewModel::setRawAddressText,
            label = { Text("Adres (address) — ör. 0x07 veya 7") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.rawExtendedAddressText,
            onValueChange = viewModel::setRawExtendedAddressText,
            label = { Text("Genişletilmiş adres (opsiyonel)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.rawCommandText,
            onValueChange = viewModel::setRawCommandText,
            label = { Text("Komut (POWER) — ör. 0x02 veya 2") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { viewModel.testRawCode() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("TEST ET (Sinyali Gönder)", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("Cihaz için isim (opsiyonel)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { viewModel.saveRawAsDevice(nickname) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Çalıştı — Cihaz Olarak Kaydet")
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Not: NEC protokolünde, bulunan adresle diğer tuşlar (ses, kanal, D-pad) için de " +
                "yaygın bir şablon otomatik uygulanır. Diğer protokollerde şimdilik yalnızca " +
                "test ettiğiniz (Güç) tuşu kaydedilir.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(24.dp))
    }
}
