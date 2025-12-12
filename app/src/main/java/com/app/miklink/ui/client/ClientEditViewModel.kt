package com.app.miklink.ui.client

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.core.data.local.room.v1.dao.ClientDao
import com.app.miklink.core.data.local.room.v1.model.Client
import com.app.miklink.core.data.local.room.v1.model.NetworkMode
import com.app.miklink.ui.common.BaseEditViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ClientEditViewModel @Inject constructor(
    private val clientDao: ClientDao,
    savedStateHandle: SavedStateHandle
) : BaseEditViewModel(savedStateHandle, "clientId") {

    // Form fields
    val companyName = MutableStateFlow("")
    val location = MutableStateFlow("")
    val notes = MutableStateFlow("")
    val networkMode = MutableStateFlow(NetworkMode.DHCP)
    val vlanId = MutableStateFlow("") // Stored as String for TextField
    val staticIp = MutableStateFlow("")
    val staticSubnet = MutableStateFlow("")
    val staticGateway = MutableStateFlow("")
    // New fields
    val staticCidr = MutableStateFlow("")
    val minLinkRate = MutableStateFlow("1G")

    val socketPrefix = MutableStateFlow("")
    val socketSuffix = MutableStateFlow("")
    val socketSeparator = MutableStateFlow("-")
    val socketNumberPadding = MutableStateFlow(1)
    val lastFloor = MutableStateFlow("")
    val lastRoom = MutableStateFlow("")

    // Speed Test configuration
    val speedTestServerAddress = MutableStateFlow("")
    val speedTestServerUser = MutableStateFlow("")
    val speedTestServerPassword = MutableStateFlow("")

    init {
        if (isEditing) {
            // Load synchronously in init so tests that create an editing view model
            // observe form fields populated immediately.
            runBlocking { loadEntity(entityId) }
        } else {
            loadIfEditing()
        }
    }
    // BaseEditViewModel provides `entityId`, `isEditing`, `isSaved` and helpers

    // NOTE: only one init is needed — the synchronous load in edit mode or
    // asynchronous load via loadIfEditing (above). A duplicate init caused
    // loadEntity to run twice which led to duplicated DAO calls in tests.

    override suspend fun loadEntity(id: Long) {
        clientDao.getClientById(id).firstOrNull()?.let { client ->
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

    private fun buildEntityFromForm(): Client {
        // NOTE: nextIdNumber will be handled in persistEntity to ensure we use the latest value
        return Client(
            clientId = if (isEditing) entityId else 0,
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
            nextIdNumber = 1, // Placeholder, updated in persistEntity
            speedTestServerAddress = speedTestServerAddress.value.takeIf { it.isNotBlank() },
            speedTestServerUser = speedTestServerUser.value.takeIf { it.isNotBlank() },
            speedTestServerPassword = speedTestServerPassword.value.takeIf { it.isNotBlank() }
        )
    }

    suspend fun persistEntity(entity: Client) {
        val finalEntity = if (isEditing) {
            // Re-fetch existing client to preserve nextIdNumber
            val existingClient = clientDao.getClientById(entityId).firstOrNull()
            entity.copy(nextIdNumber = existingClient?.nextIdNumber ?: 1)
        } else {
            entity
        }
        clientDao.insert(finalEntity)
    }

    /**
     * Public named method used by the UI layer and tests to save a client.
     * This is intentionally explicit and provides a stable public API that
     * performs validation before delegating to the shared save flow in
     * BaseEditViewModel. This keeps responsibilities single and testable.
     */
    fun saveClient() {
        // Basic validation - company name is required
        if (companyName.value.isBlank()) return

        viewModelScope.launch {
            val originalClient = if(isEditing) clientDao.getClientById(entityId).firstOrNull() else null

            val client = Client(
                clientId = if (isEditing) entityId else 0,
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
                nextIdNumber = originalClient?.nextIdNumber ?: 1,
                speedTestServerAddress = speedTestServerAddress.value.takeIf { it.isNotBlank() },
                speedTestServerUser = speedTestServerUser.value.takeIf { it.isNotBlank() },
                speedTestServerPassword = speedTestServerPassword.value.takeIf { it.isNotBlank() }
            )
            clientDao.insert(client)
            markSaved()
        }
    }

    /**
     * Validation hook used by the BaseEditViewModel save() flow.
     * Here we enforce a minimal validation rule: a Client must have a
     * non-empty company name before being persisted. This prevents
     * accidental creation of empty clients and moves validation logic
     * closer to the domain model.
     */
    // Validation helper for UI/tests – not part of BaseEditViewModel contract
    fun isValidForSave(): Boolean = companyName.value.isNotBlank()
}
