package com.atakolstudio.sure.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.atakolstudio.sure.data.local.entity.SavedDeviceEntity
import com.atakolstudio.sure.data.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val repository: DeviceRepository
) : ViewModel() {

    val devices: StateFlow<List<SavedDeviceEntity>> = repository.observeDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun renameDevice(id: Long, newName: String) {
        viewModelScope.launch { repository.renameDevice(id, newName) }
    }

    fun deleteDevice(device: SavedDeviceEntity) {
        viewModelScope.launch { repository.deleteDevice(device) }
    }
}
