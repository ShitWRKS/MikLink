package com.app.miklink.ui.client

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.NetworkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClientEditViewModel @Inject constructor(
    private val clientDao: ClientDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val clientId: Long = savedStateHandle.get<Long>("clientId") ?: -1L
    val isEditing = clientId != -1L

    // Form fields
    val companyName = MutableStateFlow("")
    val location = MutableStateFlow("")
    val notes = MutableStateFlow("")
    val networkMode = MutableStateFlow(NetworkMode.DHCP)
    val vlanId = MutableStateFlow("") // Stored as String for TextField
    val staticIp = MutableStateFlow("")
    val staticSubnet = MutableStateFlow("")
    val staticGateway = MutableStateFlow("")
    val pingTarget1 = MutableStateFlow("")
    val pingTarget2 = MutableStateFlow("")
    val pingTarget3 = MutableStateFlow("")
    val lastFloor = MutableStateFlow("")
    val lastRoom = MutableStateFlow("")


    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                clientDao.getClientById(clientId).firstOrNull()?.let { client ->
                    companyName.value = client.companyName
                    location.value = client.location ?: ""
                    notes.value = client.notes ?: ""
                    networkMode.value = NetworkMode.fromDbValue(client.networkMode)
                    vlanId.value = client.vlanId?.toString() ?: ""
                    staticIp.value = client.staticIp ?: ""
                    staticSubnet.value = client.staticSubnet ?: ""
                    staticGateway.value = client.staticGateway ?: ""
                    pingTarget1.value = client.pingTarget1 ?: ""
                    pingTarget2.value = client.pingTarget2 ?: ""
                    pingTarget3.value = client.pingTarget3 ?: ""
                    lastFloor.value = client.lastFloor ?: ""
                    lastRoom.value = client.lastRoom ?: ""
                }
            }
        }
    }

    fun saveClient() {
        viewModelScope.launch {
            // Preserve original sticky fields if not editing
            val originalClient = if(isEditing) clientDao.getClientById(clientId).firstOrNull() else null

            val client = Client(
                clientId = if (isEditing) clientId else 0,
                companyName = companyName.value,
                location = location.value.takeIf { it.isNotBlank() },
                notes = notes.value.takeIf { it.isNotBlank() },
                networkMode = networkMode.value.name,
                vlanId = vlanId.value.toIntOrNull(),
                staticIp = staticIp.value.takeIf { it.isNotBlank() && networkMode.value == NetworkMode.STATIC },
                staticSubnet = staticSubnet.value.takeIf { it.isNotBlank() && networkMode.value == NetworkMode.STATIC },
                staticGateway = staticGateway.value.takeIf { it.isNotBlank() && networkMode.value == NetworkMode.STATIC },
                pingTarget1 = pingTarget1.value.takeIf { it.isNotBlank() },
                pingTarget2 = pingTarget2.value.takeIf { it.isNotBlank() },
                pingTarget3 = pingTarget3.value.takeIf { it.isNotBlank() },
                idPrefix = originalClient?.idPrefix ?: "A",
                nextIdNumber = originalClient?.nextIdNumber ?: 1,
                lastFloor = lastFloor.value.takeIf { it.isNotBlank() },
                lastRoom = lastRoom.value.takeIf { it.isNotBlank() }
            )
            clientDao.insert(client)
            _isSaved.value = true
        }
    }
}