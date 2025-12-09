package com.app.miklink.ui.client

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.app.miklink.data.db.dao.ClientDao
import com.app.miklink.data.db.model.Client
import com.app.miklink.data.db.model.NetworkMode
import com.app.miklink.ui.common.BaseEditViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@HiltViewModel
class ClientEditViewModel @Inject constructor(
    private val clientDao: ClientDao,
    savedStateHandle: SavedStateHandle
) : BaseEditViewModel<Client>(savedStateHandle, "clientId") {

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
        loadInitialData()
    }

    override suspend fun loadEntityById(id: Long): Client? {
        return clientDao.getClientById(id).firstOrNull()
    }

    override fun populateFormFields(entity: Client) {
        companyName.value = entity.companyName
        location.value = entity.location ?: ""
        notes.value = entity.notes ?: ""
        networkMode.value = NetworkMode.fromDbValue(entity.networkMode)
        staticIp.value = entity.staticIp ?: ""
        staticSubnet.value = entity.staticSubnet ?: ""
        staticGateway.value = entity.staticGateway ?: ""
        staticCidr.value = entity.staticCidr ?: ""
        minLinkRate.value = entity.minLinkRate
        socketPrefix.value = entity.socketPrefix
        socketSuffix.value = entity.socketSuffix
        socketSeparator.value = entity.socketSeparator
        socketNumberPadding.value = entity.socketNumberPadding
        lastFloor.value = entity.lastFloor ?: ""
        lastRoom.value = entity.lastRoom ?: ""
        // Speed Test
        speedTestServerAddress.value = entity.speedTestServerAddress ?: ""
        speedTestServerUser.value = entity.speedTestServerUser ?: ""
        speedTestServerPassword.value = entity.speedTestServerPassword ?: ""
    }

    override fun buildEntityFromForm(): Client {
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

    override suspend fun persistEntity(entity: Client) {
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
        save()
    }

    /**
     * Validation hook used by the BaseEditViewModel save() flow.
     * Here we enforce a minimal validation rule: a Client must have a
     * non-empty company name before being persisted. This prevents
     * accidental creation of empty clients and moves validation logic
     * closer to the domain model.
     */
    override fun validateBeforeSave(): Boolean {
        // Keep validation minimal and deterministic; validation logic
        // can later be composed with validators or usecases.
        return companyName.value.isNotBlank()
    }
}
