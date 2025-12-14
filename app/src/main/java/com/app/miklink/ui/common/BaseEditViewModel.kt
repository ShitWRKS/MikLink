package com.app.miklink.ui.common

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Small base ViewModel to consolidate common edit-screen patterns.
 * Subclasses must implement loadEntity(id) to populate fields when editing.
 */
abstract class BaseEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val idKey: String? = "id"
) : ViewModel() {

    protected val entityId: Long = if (idKey == null) {
        -1L
    } else {
        savedStateHandle.get<Long>(idKey) ?: -1L
    }
    val isEditing: Boolean = entityId != -1L

    private val _isSaved = MutableStateFlow(false)
    val isSaved = _isSaved.asStateFlow()

    /**
     * Helper for subclasses: launch loadEntity(id) in viewModelScope if editing. This is intentionally
     * not invoked from the base class constructor because tests may not configure Dispatchers.Main.
     * Subclasses should call loadIfEditing() from their init blocks when they are ready to start
     * loading form data.
     */
    protected fun loadIfEditing() {
        if (isEditing) viewModelScope.launch { loadEntity(entityId) }
    }

    /**
     * Implement this to load the existing entity and populate form fields.
     * Called automatically when `isEditing == true`.
     */
    protected abstract suspend fun loadEntity(id: Long)

    /** Mark the form as saved. Subclasses should call this from their save methods. */
    protected fun markSaved() {
        _isSaved.value = true
    }
}

