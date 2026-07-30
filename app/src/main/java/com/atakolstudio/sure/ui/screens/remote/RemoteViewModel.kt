package com.atakolstudio.sure.ui.screens.remote

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atakolstudio.sure.data.ir.AcCodeLibrary
import com.atakolstudio.sure.data.ir.AcFanSpeed
import com.atakolstudio.sure.data.ir.AcMode
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
    /** true iken cihaz HENÜZ kaydedilmemiştir; kullanıcı "Evet, çalışıyor" demeden
     *  otomatik kaydedilmez (IR tek yönlü olduğundan uygulama gerçekten çalışıp
     *  çalışmadığını kendi başına bilemez — buna kullanıcının karar vermesi gerekir). */
    val isNewSetupNotYetSaved: Boolean = false,
    val isSavingDevice: Boolean = false,
    // --- Klima (AC) durumu ---
    val acMode: AcMode = AcMode.COOL,
    val acTemperature: Int = 22,
    val acFanSpeed: AcFanSpeed = AcFanSpeed.MED,
    val acIsOn: Boolean = false
)

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val repository: DeviceRepository,
    private val irTransmitter: IrTransmitter,
    private val acCodeLibrary: AcCodeLibrary,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    /** Bu jenerik AC profilinin desteklediği sıcaklık aralığı (UI'da kullanılır). */
    val acTemperatureRange = acCodeLibrary.supportedTemperatureRange

    init {
        val savedDeviceId = savedStateHandle.get<String>("savedDeviceId")?.toLongOrNull() ?: -1L
        val brandKeyArg = savedStateHandle.get<String>("brandKey")
        val deviceTypeArg = savedStateHandle.get<String>("deviceType")
        val connectionTypeArg = savedStateHandle.get<String>("connectionType")

        if (savedDeviceId > 0) {
            // Room'dan okuma gerektirdiği için (suspend) asenkron yüklenir.
            viewModelScope.launch { loadSavedDevice(savedDeviceId) }
        } else if (brandKeyArg != null) {
            // Saf senkron mantık; gereksiz yere bir sonraki dispatcher tick'ine
            // ertelenmemesi için doğrudan çağrılır (UI ilk karede zaten dolu görünür).
            loadNewSetup(brandKeyArg, deviceTypeArg, connectionTypeArg)
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
        val brand = if (brandKey == "generic_ac") BrandIrDatabase.GENERIC_AC_PLACEHOLDER else BrandIrDatabase.findByKey(brandKey)
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

    /**
     * Kullanıcı, kumandanın cihazını GERÇEKTEN kontrol ettiğini kendi gözüyle
     * doğruladıktan sonra çağrılır ("Evet, Çalışıyor — Kaydet" butonu). IR tek
     * yönlü bir protokol olduğundan uygulama başarıyı otomatik tespit edemez;
     * bu yüzden kayıt işlemi kullanıcının onayına bağlıdır — ilk tuşa basılınca
     * otomatik kaydedilmez.
     */
    fun confirmDeviceWorks() {
        val state = _uiState.value
        if (!state.isNewSetupNotYetSaved || state.brand == null || state.isSavingDevice) return

        _uiState.value = _uiState.value.copy(isSavingDevice = true)
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
            _uiState.value = _uiState.value.copy(
                savedDeviceId = id,
                isNewSetupNotYetSaved = false,
                isSavingDevice = false,
                lastMessage = "Cihaz kaydedildi"
            )
        }
    }

    /** Kumanda çalışmıyorsa: hiçbir şey kaydedilmeden geri dönülür (çağıran taraf onBack() çağırır). */
    fun discardUnsavedSetup() {
        _uiState.value = _uiState.value.copy(isNewSetupNotYetSaved = false)
    }

    // ------------------------------------------------------------------
    // TV / Set-üstü kutu / AVR / vb. — protokol/adres/komut tabanlı gönderim
    // ------------------------------------------------------------------

    fun sendCommand(button: RemoteButton) {
        val brand = _uiState.value.brand ?: return

        if (_uiState.value.connectionType == ConnectionType.SMART_WIFI) {
            _uiState.value = _uiState.value.copy(lastMessage = "WiFi kontrolü yakında eklenecek")
            return
        }

        val result = irTransmitter.send(brand, button)
        _uiState.value = _uiState.value.copy(lastMessage = messageFor(result))
    }

    // ------------------------------------------------------------------
    // Klima (AC) — tam durum (full-state) tabanlı gönderim
    // ------------------------------------------------------------------

    fun acSetMode(mode: AcMode) {
        _uiState.value = _uiState.value.copy(acMode = mode, acIsOn = true)
        sendCurrentAcState()
    }

    fun acIncreaseTemperature() {
        val newTemp = (_uiState.value.acTemperature + 1).coerceIn(acTemperatureRange)
        _uiState.value = _uiState.value.copy(acTemperature = newTemp, acIsOn = true)
        sendCurrentAcState()
    }

    fun acDecreaseTemperature() {
        val newTemp = (_uiState.value.acTemperature - 1).coerceIn(acTemperatureRange)
        _uiState.value = _uiState.value.copy(acTemperature = newTemp, acIsOn = true)
        sendCurrentAcState()
    }

    fun acSetFanSpeed(speed: AcFanSpeed) {
        _uiState.value = _uiState.value.copy(acFanSpeed = speed, acIsOn = true)
        sendCurrentAcState()
    }

    fun acPowerOff() {
        _uiState.value = _uiState.value.copy(acIsOn = false)
        val code = acCodeLibrary.codeForOff()
        val result = if (code == null) {
            IrTransmitResult.ButtonNotMapped
        } else {
            irTransmitter.sendRawPulses(code.frequencyHz, code.pulses)
        }
        _uiState.value = _uiState.value.copy(lastMessage = messageFor(result))
    }

    private fun sendCurrentAcState() {
        val state = _uiState.value
        val code = acCodeLibrary.codeForState(state.acMode, state.acTemperature, state.acFanSpeed)
        val result = if (code == null) {
            IrTransmitResult.ButtonNotMapped
        } else {
            irTransmitter.sendRawPulses(code.frequencyHz, code.pulses)
        }
        _uiState.value = _uiState.value.copy(lastMessage = messageFor(result))
    }

    private fun messageFor(result: IrTransmitResult): String? = when (result) {
        is IrTransmitResult.Success -> null // Başarılı gönderimde mesaj gösterme
        is IrTransmitResult.NoIrHardware -> "Bu cihazda IR verici bulunmuyor"
        is IrTransmitResult.FrequencyNotSupported -> "Bu frekans cihazınızda desteklenmiyor"
        is IrTransmitResult.ButtonNotMapped -> "Bu durum için kod bulunamadı (desteklenen aralık dışında olabilir)"
        is IrTransmitResult.Error -> "Gönderim hatası: ${result.message}"
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
