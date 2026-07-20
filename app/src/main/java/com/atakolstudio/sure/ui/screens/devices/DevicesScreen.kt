package com.atakolstudio.sure.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.atakolstudio.sure.domain.model.DeviceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onAddDeviceClick: () -> Unit,
    onDeviceClick: (Long) -> Unit,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsState()
    var deviceToRename by remember { mutableStateOf<SavedDeviceEntity?>(null) }
    var deviceToDelete by remember { mutableStateOf<SavedDeviceEntity?>(null) }
    var deviceForInfo by remember { mutableStateOf<SavedDeviceEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SURE",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cihazlarım",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddDeviceClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Yeni Cihaz Ekle") }
            )
        }
    ) { padding ->
        if (devices.isEmpty()) {
            EmptyDevicesState(modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onClick = { onDeviceClick(device.id) },
                        onRename = { deviceToRename = device },
                        onDelete = { deviceToDelete = device },
                        onInfo = { deviceForInfo = device }
                    )
                }
            }
        }
    }

    deviceToRename?.let { device ->
        RenameDeviceDialog(
            currentName = device.nickname,
            onDismiss = { deviceToRename = null },
            onConfirm = { newName ->
                viewModel.renameDevice(device.id, newName)
                deviceToRename = null
            }
        )
    }

    deviceToDelete?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceToDelete = null },
            title = { Text("Cihazı Sil") },
            text = { Text("\"${device.nickname}\" silinsin mi? Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDevice(device)
                    deviceToDelete = null
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deviceToDelete = null }) { Text("Vazgeç") }
            }
        )
    }

    deviceForInfo?.let { device ->
        AlertDialog(
            onDismissRequest = { deviceForInfo = null },
            title = { Text("Cihaz Bilgileri") },
            text = {
                Column {
                    InfoRow("İsim", device.nickname)
                    InfoRow("Marka", device.brandDisplayName)
                    InfoRow("Tür", runCatching { DeviceType.valueOf(device.deviceType).displayNameTr }.getOrDefault(device.deviceType))
                    InfoRow("Bağlantı", device.connectionType)
                }
            },
            confirmButton = {
                TextButton(onClick = { deviceForInfo = null }) { Text("Kapat") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(90.dp))
        Text(value)
    }
}

@Composable
private fun EmptyDevicesState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Tv,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text("Henüz cihaz eklemediniz", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Başlamak için sağ alttaki + düğmesine dokunun",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCard(
    device: SavedDeviceEntity,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Tv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(device.nickname, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${device.brandDisplayName}${device.model?.let { " • $it" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Seçenekler")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Cihazı Yeniden Adlandır") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onRename() }
                    )
                    DropdownMenuItem(
                        text = { Text("Özel Panoyu Düzenle") },
                        leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        onClick = { menuExpanded = false; onClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Cihaz Bilgileri") },
                        leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                        onClick = { menuExpanded = false; onInfo() }
                    )
                    DropdownMenuItem(
                        text = { Text("Cihazı Sil") },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }
    }
}

@Composable
private fun RenameDeviceDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cihazı Yeniden Adlandır") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Cihaz adı") }
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }) { Text("Kaydet") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}
