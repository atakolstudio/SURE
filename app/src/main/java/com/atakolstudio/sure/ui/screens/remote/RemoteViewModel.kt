package com.atakolstudio.sure.ui.screens.remote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atakolstudio.sure.data.ir.BrandIrCodeSet
import com.atakolstudio.sure.data.ir.BrandIrDatabase
import com.atakolstudio.sure.data.ir.IrTransmitResult
import com.atakolstudio.sure.data.ir.IrTransmitter
import com.atakolstudio.sure.data.ir.RemoteButton
import com.atakolstudio.sure.data.ir.resolveBrandIrCodeSet
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.atakolstudio.sure.data.repository.DeviceRepository
import com.atakolstudio.sure.domain.model.ConnectionType
import com.atakolstudio.sure.domain.model.DeviceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemoteUiState(
    val isLoading: Boolean = true,
    val savedDeviceId: Long? = null,
    val nickname: String = "",
    val brand: BrandIrCodeSet? = null,
    val deviceType: DeviceType = DeviceType.TV,
    val connectionType: ConnectionType = ConnectionType.TRADITIONAL_IR,
    val hasIrHardware: Boolean = true,
    val lastMessage: String? = null,
    val isNewSetupNotYetSaved: Boolean = false
)

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val irTransmitter: IrTransmitter,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    init {
        val savedDeviceId = savedStateHandle.get<String>("savedDeviceId")?.toLongOrNull() ?: -1L
        val brandKeyArg = savedStateHandle.get<String>("brandKey")
        val deviceTypeArg = savedStateHandle.get<String>("deviceType")
        val connectionTypeArg = savedStateHandle.get<String>("connectionType")

        viewModelScope.launch {
            if (savedDeviceId > 0) {
                loadSavedDevice(savedDeviceId)
            } else if (brandKeyArg != null) {
                loadNewSetup(brandKeyArg, deviceTypeArg, connectionTypeArg)
            }
        }

        _uiState.value = _uiState.value.copy(hasIrHardware = irTransmitter.hasIrEmitter)
    }

    private suspend fun loadSavedDevice(id: Long) {
        val entity = repository.getDevice(id)
        if (entity != null) {
            repository.touchLastUsed(id)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                savedDeviceId = entity.id,
                nickname = entity.nickname,
                brand = entity.resolveBrandIrCodeSet(),
                deviceType = runCatching { DeviceType.valueOf(entity.deviceType) }.getOrDefault(DeviceType.TV),
                connectionType = runCatching { ConnectionType.valueOf(entity.connectionType) }.getOrDefault(ConnectionType.TRADITIONAL_IR)
            )
        }
    }

    private fun loadNewSetup(brandKey: String, deviceTypeArg: String?, connectionTypeArg: String?) {
        val brand = BrandIrDatabase.findByKey(brandKey)
        val deviceType = runCatching { DeviceType.valueOf(deviceTypeArg ?: "TV") }.getOrDefault(DeviceType.TV)
        val connectionType = runCatching { ConnectionType.valueOf(connectionTypeArg ?: "TRADITIONAL_IR") }.getOrDefault(ConnectionType.TRADITIONAL_IR)

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            savedDeviceId = null,
            nickname = brand?.displayNameEn ?: "Yeni Cihaz",
            brand = brand,
            deviceType = deviceType,
            connectionType = connectionType,
            isNewSetupNotYetSaved = true
        )
    }

    /** Kurulum akışında kullanıcı ilk tuşa bastığında cihazı otomatik olarak kaydeder. */
    private fun persistIfNeeded() {
        val state = _uiState.value
        if (state.isNewSetupNotYetSaved && state.brand != null) {
            viewModelScope.launch {
                val now = System.currentTimeMillis()
                val id = repository.addDevice(
                    SavedDeviceEntity(
                        nickname = state.nickname,
                        brandKey = state.brand.brandKey,
                        brandDisplayName = state.brand.displayNameEn,
                        deviceType = state.deviceType.name,
                        connectionType = state.connectionType.name,
                        createdAtEpochMillis = now,
                        lastUsedEpochMillis = now
                    )
                )
                _uiState.value = _uiState.value.copy(savedDeviceId = id, isNewSetupNotYetSaved = false)
            }
        }
    }

    fun sendCommand(button: RemoteButton) {
        val brand = _uiState.value.brand ?: return
        persistIfNeeded()

        if (_uiState.value.connectionType == ConnectionType.SMART_WIFI) {
            // WiFi/akıllı cihaz protokolü bu sürümde uygulanmamıştır (bkz. README).
            _uiState.value = _uiState.value.copy(lastMessage = "WiFi kontrolü yakında eklenecek")
            return
        }

        val result = irTransmitter.send(brand, button)
        _uiState.value = _uiState.value.copy(
            lastMessage = when (result) {
                is IrTransmitResult.Success -> null // Başarılı gönderimde mesaj gösterme, sadece haptic/animasyon yeterli
                is IrTransmitResult.NoIrHardware -> "Bu cihazda IR verici bulunmuyor"
                is IrTransmitResult.FrequencyNotSupported -> "Bu frekans cihazınızda desteklenmiyor"
                is IrTransmitResult.ButtonNotMapped -> "Bu tuş, seçili marka için tanımlı değil"
                is IrTransmitResult.Error -> "Gönderim hatası: ${result.message}"
            }
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(lastMessage = null)
    }

    fun renameCurrentDevice(newName: String) {
        val id = _uiState.value.savedDeviceId ?: return
        viewModelScope.launch {
            repository.renameDevice(id, newName)
            _uiState.value = _uiState.value.copy(nickname = newName)
        }
    }
}
