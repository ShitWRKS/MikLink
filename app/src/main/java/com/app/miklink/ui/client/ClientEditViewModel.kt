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
    // Nuovi campi
    val staticCidr = MutableStateFlow("")
    val minLinkRate = MutableStateFlow("1G")

    val socketPrefix = MutableStateFlow("")
    // Nuovi campi per ID presa
    val socketSuffix = MutableStateFlow("")
    val socketSeparator = MutableStateFlow("-")
    val socketNumberPadding = MutableStateFlow(1)
    val lastFloor = MutableStateFlow("")
    val lastRoom = MutableStateFlow("")

    // Speed Test configuration
    val speedTestServerAddress = MutableStateFlow("")
    val speedTestServerUser = MutableStateFlow("")
    val speedTestServerPassword = MutableStateFlow("")

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
                    staticIp.value = client.staticIp ?: ""
                    staticSubnet.value = client.staticSubnet ?: ""
                    staticGateway.value = client.staticGateway ?: ""
                    staticCidr.value = client.staticCidr ?: ""
                    minLinkRate.value = client.minLinkRate
                    socketPrefix.value = client.socketPrefix
                        socketSuffix.value = client.socketSuffix
                        socketSeparator.value = client.socketSeparator
                        socketNumberPadding.value = client.socketNumberPadding
                    lastFloor.value = client.lastFloor ?: ""
                    lastRoom.value = client.lastRoom ?: ""
                    // Speed Test
                    speedTestServerAddress.value = client.speedTestServerAddress ?: ""
                    speedTestServerUser.value = client.speedTestServerUser ?: ""
                    speedTestServerPassword.value = client.speedTestServerPassword ?: ""
                }
            }
        }
    }

    fun saveClient() {
        viewModelScope.launch {
            val originalClient = if(isEditing) clientDao.getClientById(clientId).firstOrNull() else null

            val client = Client(
                clientId = if (isEditing) clientId else 0,
                companyName = companyName.value,
                location = location.value.takeIf { it.isNotBlank() },
                notes = notes.value.takeIf { it.isNotBlank() },
                networkMode = networkMode.value.name,
                staticIp = staticIp.value.takeIf { it.isNotBlank() && networkMode.value == NetworkMode.STATIC },
                staticSubnet = staticSubnet.value.takeIf { it.isNotBlank() && networkMode.value == NetworkMode.STATIC },
                staticGateway = staticGateway.value.takeIf { it.isNotBlank() && networkMode.value == NetworkMode.STATIC },
                staticCidr = staticCidr.value.takeIf { it.isNotBlank() && networkMode.value == NetworkMode.STATIC },
                minLinkRate = minLinkRate.value,
                socketPrefix = socketPrefix.value,
                socketSuffix = socketSuffix.value,
                socketSeparator = socketSeparator.value,
                socketNumberPadding = socketNumberPadding.value,
                nextIdNumber = originalClient?.nextIdNumber ?: 1,
                speedTestServerAddress = speedTestServerAddress.value.takeIf { it.isNotBlank() },
                speedTestServerUser = speedTestServerUser.value.takeIf { it.isNotBlank() },
                speedTestServerPassword = speedTestServerPassword.value.takeIf { it.isNotBlank() }
            )
            clientDao.insert(client)
            _isSaved.value = true
        }
    }
}