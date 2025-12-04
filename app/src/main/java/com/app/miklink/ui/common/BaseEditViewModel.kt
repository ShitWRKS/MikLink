package com.app.miklink.ui.common

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Base ViewModel for edit screens that follow the common pattern:
 * - Load entity by ID from database if editing
 * - Manage form fields as StateFlows
 * - Save entity back to database
 * 
 * @param T The entity type being edited
 * @param savedStateHandle The SavedStateHandle containing navigation arguments
 * @param idKey The key used to retrieve the entity ID from savedStateHandle (default: "id")
 */
abstract class BaseEditViewModel<T>(
    savedStateHandle: SavedStateHandle,
    private val idKey: String = "id"
) : ViewModel() {

    /**
     * The entity ID from navigation arguments, or -1L if creating a new entity
     */
    protected val entityId: Long = savedStateHandle.get<Long>(idKey) ?: -1L

    /**
     * Whether we're editing an existing entity (true) or creating a new one (false)
     */
    val isEditing: Boolean = entityId != -1L

    /**
     * Tracks whether the entity has been saved successfully
     */
    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    /**
     * Loads the entity data if in editing mode.
     * Child classes MUST call this from their init block to ensure proper initialization order.
     * 
     * Example:
     * ```
     * init {
     *     loadInitialData()
     * }
     * ```
     */
    protected fun loadInitialData() {
        if (isEditing) {
            viewModelScope.launch {
                loadEntityById(entityId)?.let { entity ->
                    populateFormFields(entity)
                }
            }
        }
    }

    /**
     * Load the entity from the database by ID.
     * This should return a Flow and get the first value, or null if not found.
     * 
     * Example implementation:
     * ```
     * override suspend fun loadEntityById(id: Long): T? {
     *     return dao.getById(id).firstOrNull()
     * }
     * ```
     */
    protected abstract suspend fun loadEntityById(id: Long): T?

    /**
     * Populate the form fields from the loaded entity.
     * This is called after loadEntityById returns a non-null entity.
     * 
     * Example implementation:
     * ```
     * override fun populateFormFields(entity: Client) {
     *     companyName.value = entity.companyName
     *     location.value = entity.location ?: ""
     *     // ... etc
     * }
     * ```
     */
    protected abstract fun populateFormFields(entity: T)

    /**
     * Create an entity instance from the current form field values.
     * This is called when saving the entity.
     * 
     * Example implementation:
     * ```
     * override fun buildEntityFromForm(): Client {
     *     return Client(
     *         clientId = if (isEditing) entityId else 0,
     *         companyName = companyName.value,
     *         location = location.value.takeIf { it.isNotBlank() }
     *         // ... etc
     *     )
     * }
     * ```
     */
    protected abstract fun buildEntityFromForm(): T

    /**
     * Persist the entity to the database.
     * This is called by the public save() method.
     * 
     * Example implementation:
     * ```
     * override suspend fun persistEntity(entity: Client) {
     *     dao.insert(entity)
     * }
     * ```
     */
    protected abstract suspend fun persistEntity(entity: T)

    /**
     * Optional hook called before saving, for validation or preprocessing.
     * Return true to continue with save, false to abort.
     * Default implementation returns true.
     */
    protected open fun validateBeforeSave(): Boolean = true

    /**
     * Optional hook called after successful save.
     * Default implementation does nothing.
     */
    protected open suspend fun onSaveSuccess(entity: T) {
        // Override if needed
    }

    /**
     * Save the entity to the database.
     * This is the public method that screens call.
     */
    fun save() {
        if (!validateBeforeSave()) {
            return
        }

        viewModelScope.launch {
            val entity = buildEntityFromForm()
            persistEntity(entity)
            _isSaved.value = true
            onSaveSuccess(entity)
        }
    }
}
