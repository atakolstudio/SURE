package com.atakolstudio.sure.ui.screens.devices

import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.atakolstudio.sure.testutil.FakeDeviceRepository
import com.atakolstudio.sure.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DevicesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun sampleDevice(nickname: String) = SavedDeviceEntity(
        nickname = nickname,
        brandKey = "samsung",
        brandDisplayName = "Samsung",
        deviceType = "TV",
        connectionType = "TRADITIONAL_IR",
        createdAtEpochMillis = 0L,
        lastUsedEpochMillis = 0L
    )

    @Test
    fun `bos veritabaninda devices akisi bos liste yayar`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeDeviceRepository()
        val viewModel = DevicesViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.devices.value).isEmpty()
    }

    @Test
    fun `eklenen cihazlar devices akisinda gorunur`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeDeviceRepository()
        repository.addDevice(sampleDevice("Salon TV"))
        val viewModel = DevicesViewModel(repository)
        advanceUntilIdle()

        assertThat(viewModel.devices.value).hasSize(1)
        assertThat(viewModel.devices.value.first().nickname).isEqualTo("Salon TV")
    }

    @Test
    fun `renameDevice cihazin ismini gunceller`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeDeviceRepository()
        val id = repository.addDevice(sampleDevice("Eski İsim"))
        val viewModel = DevicesViewModel(repository)
        advanceUntilIdle()

        viewModel.renameDevice(id, "Yeni İsim")
        advanceUntilIdle()

        assertThat(viewModel.devices.value.first().nickname).isEqualTo("Yeni İsim")
    }

    @Test
    fun `deleteDevice cihazi listeden kaldirir`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeDeviceRepository()
        repository.addDevice(sampleDevice("Silinecek Cihaz"))
        val viewModel = DevicesViewModel(repository)
        advanceUntilIdle()
        val deviceToDelete = viewModel.devices.value.first()

        viewModel.deleteDevice(deviceToDelete)
        advanceUntilIdle()

        assertThat(viewModel.devices.value).isEmpty()
    }
}
