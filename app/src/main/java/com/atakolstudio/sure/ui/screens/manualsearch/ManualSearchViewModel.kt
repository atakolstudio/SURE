package com.atakolstudio.sure.ui.screens.manualsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atakolstudio.sure.data.ir.BrandIrCodeSet
import com.atakolstudio.sure.data.ir.BrandIrDatabase
import com.atakolstudio.sure.data.ir.CUSTOM_BRAND_KEY
import com.atakolstudio.sure.data.ir.IrCommand
import com.atakolstudio.sure.data.ir.IrProtocol
import com.atakolstudio.sure.data.ir.IrTransmitResult
import com.atakolstudio.sure.data.ir.IrTransmitter
import com.atakolstudio.sure.data.ir.RemoteButton
import com.atakolstudio.sure.data.ir.buildNecTemplateCommands
import com.atakolstudio.sure.data.ir.toJsonString
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

enum class ManualSearchMode { SCAN, RAW_ENTRY }

data class ManualSearchUiState(
    val mode: ManualSearchMode = ManualSearchMode.SCAN,
    val deviceType: DeviceType = DeviceType.TV,
    val connectionType: ConnectionType = ConnectionType.TRADITIONAL_IR,
    val candidates: List<BrandIrCodeSet> = BrandIrDatabase.brands,
    val currentIndex: Int = 0,
    val exhausted: Boolean = false,
    val hasIrHardware: Boolean = true,
    val lastMessage: String? = null,
    val savedDeviceId: Long? = null,
    // Elle kod girme alanları
    val rawProtocol: IrProtocol = IrProtocol.NEC,
    val rawAddressText: String = "",
    val rawExtendedAddressText: String = "",
    val rawCommandText: String = ""
) {
    val currentCandidate: BrandIrCodeSet? get() = candidates.getOrNull(currentIndex)
}

@HiltViewModel
class ManualSearchViewModel @Inject constructor(
    private val irTransmitter: IrTransmitter,
    private val repository: DeviceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState: MutableStateFlow<ManualSearchUiState>

    init {
        val deviceType = runCatching {
            DeviceType.valueOf(savedStateHandle.get<String>("deviceType") ?: "TV")
        }.getOrDefault(DeviceType.TV)
        val connectionType = runCatching {
            ConnectionType.valueOf(savedStateHandle.get<String>("connectionType") ?: "TRADITIONAL_IR")
        }.getOrDefault(ConnectionType.TRADITIONAL_IR)

        _uiState = MutableStateFlow(
            ManualSearchUiState(
                deviceType = deviceType,
                connectionType = connectionType,
                hasIrHardware = irTransmitter.hasIrEmitter
            )
        )
    }

    val uiState: StateFlow<ManualSearchUiState> = _uiState.asStateFlow()

    fun setMode(mode: ManualSearchMode) {
        _uiState.value = _uiState.value.copy(mode = mode, lastMessage = null)
    }

    // ------------------------------------------------------------------
    // MOD 1: Kod Tarama (Marka listesinde sırayla dene)
    // ------------------------------------------------------------------

    fun testCurrentCandidate() {
        val candidate = _uiState.value.currentCandidate ?: return
        val result = irTransmitter.send(candidate, RemoteButton.POWER)
        _uiState.value = _uiState.value.copy(lastMessage = messageFor(result))
    }

    fun nextCandidate() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.candidates.size) {
            _uiState.value = state.copy(exhausted = true)
        } else {
            _uiState.value = state.copy(currentIndex = nextIndex, lastMessage = null)
        }
    }

    fun previousCandidate() {
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1, lastMessage = null, exhausted = false)
        }
    }

    fun restartScan() {
        _uiState.value = _uiState.value.copy(currentIndex = 0, exhausted = false, lastMessage = null)
    }

    fun confirmCurrentMatch(nickname: String) {
        val state = _uiState.value
        val brand = state.currentCandidate ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = repository.addDevice(
                SavedDeviceEntity(
                    nickname = nickname.ifBlank { brand.displayNameEn },
                    brandKey = brand.brandKey,
                    brandDisplayName = brand.displayNameEn,
                    deviceType = state.deviceType.name,
                    connectionType = state.connectionType.name,
                    createdAtEpochMillis = now,
                    lastUsedEpochMillis = now
                )
            )
            _uiState.value = _uiState.value.copy(savedDeviceId = id)
        }
    }

    // ------------------------------------------------------------------
    // MOD 2: Elle IR Kodu Girme (protokol + adres + komut)
    // ------------------------------------------------------------------

    fun setRawProtocol(protocol: IrProtocol) {
        _uiState.value = _uiState.value.copy(rawProtocol = protocol, lastMessage = null)
    }

    fun setRawAddressText(text: String) {
        _uiState.value = _uiState.value.copy(rawAddressText = text)
    }

    fun setRawExtendedAddressText(text: String) {
        _uiState.value = _uiState.value.copy(rawExtendedAddressText = text)
    }

    fun setRawCommandText(text: String) {
        _uiState.value = _uiState.value.copy(rawCommandText = text)
    }

    /** Kullanıcının girdiği ham protokol/adres/komut ile bir test sinyali gönderir. */
    fun testRawCode() {
        val state = _uiState.value
        val address = parseFlexibleInt(state.rawAddressText)
        val command = parseFlexibleInt(state.rawCommandText)
        val extendedAddress = parseFlexibleInt(state.rawExtendedAddressText)

        if (address == null || command == null) {
            _uiState.value = state.copy(lastMessage = "Lütfen geçerli bir adres ve komut değeri girin (ör. 0x1A veya 26)")
            return
        }

        val command1 = IrCommand(
            protocol = state.rawProtocol,
            address = address,
            command = command,
            extendedAddress = extendedAddress
        )
        val result = irTransmitter.sendRaw(command1)
        _uiState.value = _uiState.value.copy(lastMessage = messageFor(result))
    }

    /** Test edilen ham kodu, kullanıcı onayladıktan sonra yeni bir cihaz olarak kaydeder. */
    fun saveRawAsDevice(nickname: String) {
        val state = _uiState.value
        val address = parseFlexibleInt(state.rawAddressText) ?: return
        val command = parseFlexibleInt(state.rawCommandText) ?: return
        val extendedAddress = parseFlexibleInt(state.rawExtendedAddressText)

        // NEC ailesinde, bulunan tek adresle diğer tuşlar için de makul bir şablon
        // üretebiliyoruz (bkz. buildNecTemplateCommands). Diğer protokollerde ise
        // sadece test edilen (genelde POWER) tuşu kaydedilir.
        val commands: Map<RemoteButton, Int> = if (state.rawProtocol == IrProtocol.NEC) {
            buildNecTemplateCommands(testedPowerCommand = command)
        } else {
            mapOf(RemoteButton.POWER to command)
        }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val displayName = nickname.ifBlank { "Manuel Cihaz" }
            val id = repository.addDevice(
                SavedDeviceEntity(
                    nickname = displayName,
                    brandKey = CUSTOM_BRAND_KEY,
                    brandDisplayName = displayName,
                    deviceType = state.deviceType.name,
                    connectionType = state.connectionType.name,
                    createdAtEpochMillis = now,
                    lastUsedEpochMillis = now,
                    customProtocol = state.rawProtocol.name,
                    customAddress = address,
                    customExtendedAddress = extendedAddress,
                    customCommandsJson = commands.toJsonString()
                )
            )
            _uiState.value = _uiState.value.copy(savedDeviceId = id)
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(lastMessage = null)
    }

    private fun messageFor(result: IrTransmitResult): String? = when (result) {
        is IrTransmitResult.Success -> null
        is IrTransmitResult.NoIrHardware -> "Bu cihazda IR verici bulunmuyor"
        is IrTransmitResult.FrequencyNotSupported -> "Bu frekans cihazınızda desteklenmiyor"
        is IrTransmitResult.ButtonNotMapped -> "Bu tuş bu marka için tanımlı değil"
        is IrTransmitResult.Error -> "Gönderim hatası: ${result.message}"
    }

    private fun parseFlexibleInt(text: String): Int? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return try {
            if (trimmed.startsWith("0x", ignoreCase = true)) {
                trimmed.substring(2).toInt(16)
            } else {
                trimmed.toIntOrNull() ?: trimmed.toInt(16)
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}
