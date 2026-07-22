package com.atakolstudio.sure.ui.screens.manualsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atakolstudio.sure.data.ir.BlindScanCandidates
import com.atakolstudio.sure.data.ir.BrandIrCodeSet
import com.atakolstudio.sure.data.ir.BrandIrDatabase
import com.atakolstudio.sure.data.ir.CUSTOM_BRAND_KEY
import com.atakolstudio.sure.data.ir.IrCommand
import com.atakolstudio.sure.data.ir.IrProtocol
import com.atakolstudio.sure.data.ir.IrTransmitResult
import com.atakolstudio.sure.data.ir.IrTransmitter
import com.atakolstudio.sure.data.ir.LircBlindScanLoader
import com.atakolstudio.sure.data.ir.RemoteButton
import com.atakolstudio.sure.data.ir.buildNecTemplateCommands
import com.atakolstudio.sure.data.ir.toJsonString
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.atakolstudio.sure.data.repository.DeviceRepository
import com.atakolstudio.sure.domain.model.ConnectionType
import com.atakolstudio.sure.domain.model.DeviceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
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
    val blindScanEnabled: Boolean = false,
    val extremeScanEnabled: Boolean = false,
    val isAutoScanning: Boolean = false,
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
    private val lircBlindScanLoader: LircBlindScanLoader,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState: MutableStateFlow<ManualSearchUiState>
    private var autoScanJob: Job? = null

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
        stopAutoScan()
        _uiState.value = _uiState.value.copy(mode = mode, lastMessage = null)
    }

    // ------------------------------------------------------------------
    // MOD 1: Kod Tarama (bilinen markalar + isteğe bağlı Kör Tarama)
    // ------------------------------------------------------------------

    /**
     * Kör Tarama açıldığında, veritabanındaki bilinen ~25 markaya ek olarak,
     * LIRC (Linux Infrared Remote Control) açık kaynak veritabanından derlenmiş
     * ~371 GERÇEK kumanda kodu eklenir (bkz. LircBlindScanLoader). Bu kodlar, gerçek
     * kumandalardan okunmuştur; bu yüzden rastgele üretilmiş bir kod ızgarasından çok
     * daha yüksek eşleşme ihtimaline sahiptir ve çoğu zaman güç dışında ses, kanal,
     * D-pad gibi diğer tuşları da içerir.
     */
    fun setBlindScanEnabled(enabled: Boolean) {
        stopAutoScan()
        _uiState.value = _uiState.value.copy(
            blindScanEnabled = enabled,
            currentIndex = 0,
            exhausted = false,
            lastMessage = null
        )
        rebuildCandidates()
    }

    /**
     * Ekstra "Aşırı Tarama": LIRC veritabanında da karşılığı çıkmayan, gerçekten
     * isimsiz/kataloglanmamış cihazlar için son çare. NEC/Sony protokollerinde
     * sistematik bir adres × komut ızgarası dener (binlerce kombinasyon, yalnızca
     * güç tuşu test edilir). LIRC verisinden çok daha düşük isabet ihtimaline
     * sahiptir, bu yüzden ayrı ve varsayılan olarak kapalı bir anahtardır.
     */
    fun setExtremeScanEnabled(enabled: Boolean) {
        stopAutoScan()
        _uiState.value = _uiState.value.copy(
            extremeScanEnabled = enabled,
            currentIndex = 0,
            exhausted = false,
            lastMessage = null
        )
        rebuildCandidates()
    }

    private fun rebuildCandidates() {
        val state = _uiState.value
        val candidates = buildList {
            addAll(BrandIrDatabase.brands)
            if (state.blindScanEnabled) {
                addAll(lircBlindScanLoader.loadCandidates())
            }
            if (state.extremeScanEnabled) {
                addAll(BlindScanCandidates.generateFullBlindScan())
            }
        }
        _uiState.value = _uiState.value.copy(candidates = candidates)
    }

    fun testCurrentCandidate() {
        val candidate = _uiState.value.currentCandidate ?: return
        val result = irTransmitter.send(candidate, RemoteButton.POWER)
        _uiState.value = _uiState.value.copy(lastMessage = messageFor(result))
    }

    fun nextCandidate() {
        stopAutoScan()
        advanceToNext()
    }

    private fun advanceToNext() {
        val state = _uiState.value
        val nextIndex = state.currentIndex + 1
        if (nextIndex >= state.candidates.size) {
            _uiState.value = state.copy(exhausted = true, isAutoScanning = false)
        } else {
            _uiState.value = state.copy(currentIndex = nextIndex, lastMessage = null)
        }
    }

    fun previousCandidate() {
        stopAutoScan()
        val state = _uiState.value
        if (state.currentIndex > 0) {
            _uiState.value = state.copy(currentIndex = state.currentIndex - 1, lastMessage = null, exhausted = false)
        }
    }

    fun restartScan() {
        stopAutoScan()
        _uiState.value = _uiState.value.copy(currentIndex = 0, exhausted = false, lastMessage = null)
    }

    /**
     * Otomatik tarama: elle "Sıradaki" tıklamak yerine, belirli aralıklarla
     * kendiliğinden test gönderip ilerler. Kullanıcı cihaz tepki verdiği an
     * durdurup "Evet, Bu Doğru" ile onaylar.
     */
    fun toggleAutoScan() {
        if (_uiState.value.isAutoScanning) {
            stopAutoScan()
        } else {
            startAutoScan()
        }
    }

    private fun startAutoScan() {
        if (autoScanJob?.isActive == true) return
        _uiState.value = _uiState.value.copy(isAutoScanning = true, exhausted = false)
        autoScanJob = viewModelScope.launch {
            while (isActive && _uiState.value.isAutoScanning) {
                val state = _uiState.value
                if (state.exhausted) {
                    _uiState.value = state.copy(isAutoScanning = false)
                    break
                }
                testCurrentCandidate()
                delay(1200)
                if (!_uiState.value.isAutoScanning) break
                advanceToNext()
            }
        }
    }

    fun stopAutoScan() {
        autoScanJob?.cancel()
        autoScanJob = null
        if (_uiState.value.isAutoScanning) {
            _uiState.value = _uiState.value.copy(isAutoScanning = false)
        }
    }

    fun confirmCurrentMatch(nickname: String) {
        stopAutoScan()
        val state = _uiState.value
        val brand = state.currentCandidate ?: return
        val isNamedBrand = BrandIrDatabase.brands.any { it.brandKey == brand.brandKey }

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val entity = if (isNamedBrand) {
                SavedDeviceEntity(
                    nickname = nickname.ifBlank { brand.displayNameEn },
                    brandKey = brand.brandKey,
                    brandDisplayName = brand.displayNameEn,
                    deviceType = state.deviceType.name,
                    connectionType = state.connectionType.name,
                    createdAtEpochMillis = now,
                    lastUsedEpochMillis = now
                )
            } else {
                // Kör taramadan bulunan, veritabanında adıyla tanımlı olmayan bir cihaz.
                // "Custom" olarak, bulunan protokol/adres ile kaydedilir. LIRC kaynaklı
                // adaylar genelde zaten zengin (çok tuşlu) bir komut haritasına sahiptir;
                // yalnızca tek tuşlu (sentetik "Aşırı Tarama") adaylarda NEC şablonu uygulanır.
                val fullCommands = if (brand.commands.size <= 1 && brand.protocol == IrProtocol.NEC) {
                    buildNecTemplateCommands(testedPowerCommand = brand.commands[RemoteButton.POWER] ?: 0)
                } else {
                    brand.commands
                }
                val displayName = nickname.ifBlank { "Bulunan Cihaz" }
                SavedDeviceEntity(
                    nickname = displayName,
                    brandKey = CUSTOM_BRAND_KEY,
                    brandDisplayName = displayName,
                    deviceType = state.deviceType.name,
                    connectionType = state.connectionType.name,
                    createdAtEpochMillis = now,
                    lastUsedEpochMillis = now,
                    customProtocol = brand.protocol.name,
                    customAddress = brand.address,
                    customExtendedAddress = brand.extendedAddress,
                    customCommandsJson = fullCommands.toJsonString()
                )
            }
            val id = repository.addDevice(entity)
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

    override fun onCleared() {
        super.onCleared()
        autoScanJob?.cancel()
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
