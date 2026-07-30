package com.atakolstudio.sure.ui.screens.remote

import androidx.lifecycle.SavedStateHandle
import com.atakolstudio.sure.data.ir.AcCodeLibrary
import com.atakolstudio.sure.data.ir.AcFanSpeed
import com.atakolstudio.sure.data.ir.AcMode
import com.atakolstudio.sure.data.ir.IrTransmitResult
import com.atakolstudio.sure.data.ir.IrTransmitter
import com.atakolstudio.sure.data.ir.RemoteButton
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.atakolstudio.sure.testutil.FakeDeviceRepository
import com.atakolstudio.sure.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RemoteViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var irTransmitter: IrTransmitter
    private lateinit var acCodeLibrary: AcCodeLibrary
    private lateinit var repository: FakeDeviceRepository

    private val fakeAcPulseCode = AcCodeLibrary.AcPulseCode(38000, intArrayOf(1000, 2000, 1000))

    @Before
    fun setUp() {
        irTransmitter = mockk(relaxed = true)
        every { irTransmitter.send(any(), any()) } returns IrTransmitResult.Success
        every { irTransmitter.sendRawPulses(any(), any()) } returns IrTransmitResult.Success

        acCodeLibrary = mockk()
        every { acCodeLibrary.supportedTemperatureRange } returns 17..25
        every { acCodeLibrary.codeForState(any(), any(), any()) } returns fakeAcPulseCode
        every { acCodeLibrary.codeForOff() } returns fakeAcPulseCode
        every { acCodeLibrary.codeForFanOnly(any()) } returns fakeAcPulseCode

        repository = FakeDeviceRepository()
    }

    private fun createViewModel(
        savedDeviceId: Long? = null,
        brandKey: String? = "samsung",
        deviceType: String = "TV",
        connectionType: String = "TRADITIONAL_IR"
    ): RemoteViewModel {
        val args = mutableMapOf<String, Any?>(
            "deviceType" to deviceType,
            "connectionType" to connectionType
        )
        if (savedDeviceId != null) args["savedDeviceId"] = savedDeviceId.toString()
        if (brandKey != null) args["brandKey"] = brandKey
        return RemoteViewModel(repository, irTransmitter, acCodeLibrary, SavedStateHandle(args))
    }

    // ------------------------------------------------------------------
    // TV / genel komut gönderimi
    // ------------------------------------------------------------------

    @Test
    fun `yeni kurulumda marka ve cihaz turu dogru yuklenir`() {
        val viewModel = createViewModel(brandKey = "samsung", deviceType = "TV")
        val state = viewModel.uiState.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.brand?.brandKey).isEqualTo("samsung")
        assertThat(state.deviceType).isEqualTo(com.atakolstudio.sure.domain.model.DeviceType.TV)
        assertThat(state.isNewSetupNotYetSaved).isTrue()
    }

    @Test
    fun `sendCommand IrTransmitter'i cagirir`() {
        val viewModel = createViewModel(brandKey = "samsung")
        viewModel.sendCommand(RemoteButton.POWER)

        verify { irTransmitter.send(any(), RemoteButton.POWER) }
    }

    @Test
    fun `sendCommand cagrilsa bile kullanici onaylamadan cihaz kaydedilmez`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(brandKey = "samsung")
        viewModel.sendCommand(RemoteButton.POWER)
        advanceUntilIdle()

        // IR tek yönlü olduğundan, uygulama tuşa basılmasından "çalıştığını" anlayamaz;
        // kullanıcı açıkça onaylamadan hiçbir şey kaydedilmemelidir.
        assertThat(repository.currentDevices).isEmpty()
        assertThat(viewModel.uiState.value.isNewSetupNotYetSaved).isTrue()
        assertThat(viewModel.uiState.value.savedDeviceId).isNull()
    }

    @Test
    fun `confirmDeviceWorks cagrilinca cihaz kaydedilir`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(brandKey = "samsung")
        viewModel.sendCommand(RemoteButton.POWER)

        viewModel.confirmDeviceWorks()
        advanceUntilIdle()

        assertThat(repository.currentDevices).hasSize(1)
        assertThat(repository.currentDevices.first().brandKey).isEqualTo("samsung")
        assertThat(viewModel.uiState.value.isNewSetupNotYetSaved).isFalse()
        assertThat(viewModel.uiState.value.savedDeviceId).isNotNull()
    }

    @Test
    fun `discardUnsavedSetup hicbir sey kaydetmeden kurulum bayragini temizler`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = createViewModel(brandKey = "samsung")
        viewModel.sendCommand(RemoteButton.POWER)

        viewModel.discardUnsavedSetup()
        advanceUntilIdle()

        assertThat(repository.currentDevices).isEmpty()
        assertThat(viewModel.uiState.value.isNewSetupNotYetSaved).isFalse()
    }

    @Test
    fun `kayitli (mevcut) bir cihazda confirmDeviceWorks hicbir sey yapmaz`() = runTest(mainDispatcherRule.testDispatcher) {
        val now = System.currentTimeMillis()
        val savedId = repository.addDevice(
            SavedDeviceEntity(
                nickname = "Salon TV", brandKey = "lg", brandDisplayName = "LG",
                deviceType = "TV", connectionType = "TRADITIONAL_IR",
                createdAtEpochMillis = now, lastUsedEpochMillis = now
            )
        )
        val viewModel = createViewModel(savedDeviceId = savedId, brandKey = null)
        advanceUntilIdle()

        viewModel.confirmDeviceWorks()
        advanceUntilIdle()

        // isNewSetupNotYetSaved zaten false olduğundan ikinci bir kayıt oluşturulmamalı
        assertThat(repository.currentDevices).hasSize(1)
    }

    @Test
    fun `WiFi baglantili cihazda IR gonderilmez, bilgi mesaji gosterilir`() {
        val viewModel = createViewModel(brandKey = "samsung", connectionType = "SMART_WIFI")
        viewModel.sendCommand(RemoteButton.POWER)

        assertThat(viewModel.uiState.value.lastMessage).isNotNull()
        io.mockk.verify(exactly = 0) { irTransmitter.send(any(), any()) }
    }

    @Test
    fun `kayitli cihaz yuklendiginde repository'den dogru veri gelir`() = runTest(mainDispatcherRule.testDispatcher) {
        val now = System.currentTimeMillis()
        val savedId = repository.addDevice(
            SavedDeviceEntity(
                nickname = "Salon TV",
                brandKey = "lg",
                brandDisplayName = "LG",
                deviceType = "TV",
                connectionType = "TRADITIONAL_IR",
                createdAtEpochMillis = now,
                lastUsedEpochMillis = now
            )
        )

        val viewModel = createViewModel(savedDeviceId = savedId, brandKey = null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.nickname).isEqualTo("Salon TV")
        assertThat(state.brand?.brandKey).isEqualTo("lg")
        assertThat(state.isLoading).isFalse()
    }

    // ------------------------------------------------------------------
    // Klima (AC) — tam durum yönetimi
    // ------------------------------------------------------------------

    @Test
    fun `klima varsayilan durumu Sogutma 22 derece Orta fan`() {
        val viewModel = createViewModel(brandKey = "generic_ac", deviceType = "AC")
        val state = viewModel.uiState.value

        assertThat(state.acMode).isEqualTo(AcMode.COOL)
        assertThat(state.acTemperature).isEqualTo(22)
        assertThat(state.acFanSpeed).isEqualTo(AcFanSpeed.MED)
        assertThat(state.acIsOn).isFalse()
    }

    @Test
    fun `sicaklik artirma ust sinira kadar calisir`() {
        val viewModel = createViewModel(brandKey = "generic_ac", deviceType = "AC")
        repeat(10) { viewModel.acIncreaseTemperature() }

        assertThat(viewModel.uiState.value.acTemperature).isEqualTo(25) // ust sinir
    }

    @Test
    fun `sicaklik azaltma alt sinira kadar calisir`() {
        val viewModel = createViewModel(brandKey = "generic_ac", deviceType = "AC")
        repeat(10) { viewModel.acDecreaseTemperature() }

        assertThat(viewModel.uiState.value.acTemperature).isEqualTo(17) // alt sinir
    }

    @Test
    fun `sicaklik degistiginde acIsOn true olur ve IR gonderilir`() {
        val viewModel = createViewModel(brandKey = "generic_ac", deviceType = "AC")
        viewModel.acIncreaseTemperature()

        assertThat(viewModel.uiState.value.acIsOn).isTrue()
        verify { irTransmitter.sendRawPulses(38000, fakeAcPulseCode.pulses) }
    }

    @Test
    fun `mod degistirme dogru moda gecirir`() {
        val viewModel = createViewModel(brandKey = "generic_ac", deviceType = "AC")
        viewModel.acSetMode(AcMode.HEAT)

        assertThat(viewModel.uiState.value.acMode).isEqualTo(AcMode.HEAT)
        assertThat(viewModel.uiState.value.acIsOn).isTrue()
    }

    @Test
    fun `fan hizi degistirme dogru hiza gecirir`() {
        val viewModel = createViewModel(brandKey = "generic_ac", deviceType = "AC")
        viewModel.acSetFanSpeed(AcFanSpeed.HIGH)

        assertThat(viewModel.uiState.value.acFanSpeed).isEqualTo(AcFanSpeed.HIGH)
    }

    @Test
    fun `guc kapatma acIsOn'u false yapar ve OFF kodu gonderir`() {
        val viewModel = createViewModel(brandKey = "generic_ac", deviceType = "AC")
        viewModel.acSetMode(AcMode.COOL) // once ac'yi ac
        viewModel.acPowerOff()

        assertThat(viewModel.uiState.value.acIsOn).isFalse()
        verify { irTransmitter.sendRawPulses(38000, fakeAcPulseCode.pulses) }
    }

    @Test
    fun `kod bulunamazsa kullaniciya bilgi mesaji gosterilir`() {
        every { acCodeLibrary.codeForState(any(), any(), any()) } returns null
        val viewModel = createViewModel(brandKey = "generic_ac", deviceType = "AC")

        viewModel.acSetMode(AcMode.COOL)

        assertThat(viewModel.uiState.value.lastMessage).isNotNull()
    }
}
