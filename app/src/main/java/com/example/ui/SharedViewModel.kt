package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Note
import com.example.data.NoteRepository
import com.example.data.SecurityPreferences
import com.example.security.PinUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.catch
import java.io.IOException

class SharedViewModel(
    private val repository: NoteRepository,
    val securityPrefs: SecurityPreferences
) : ViewModel() {

    val allNotes: StateFlow<List<Note>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storedPinHash: StateFlow<String?> = securityPrefs.hashedPinFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val fakePinHash: StateFlow<String?> = securityPrefs.hashedFakePinFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        
    val securityQuestion: StateFlow<String?> = securityPrefs.securityQuestionFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
        
    val hashedSecurityAnswer: StateFlow<String?> = securityPrefs.hashedSecurityAnswerFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isAutoSaveEnabled: StateFlow<Boolean> = securityPrefs.isAutoSaveEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isBiometricSimulationEnabled: StateFlow<Boolean> = securityPrefs.isBiometricSimulationEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val autoSaveInterval: StateFlow<Int> = securityPrefs.autoSaveIntervalFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 5)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val isFakeVaultActive = MutableStateFlow(false)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun verifyPin(pin: String): Boolean {
        val hashedInput = PinUtils.hashPin(pin)
        val storedHash = securityPrefs.hashedPinFlow.firstOrNull()
        val fakeHash = securityPrefs.hashedFakePinFlow.firstOrNull()
        
        return if (storedHash != null && hashedInput == storedHash) {
            isFakeVaultActive.value = false
            true
        } else if (fakeHash != null && hashedInput == fakeHash) {
            isFakeVaultActive.value = true
            true
        } else {
            false
        }
    }
    
    suspend fun verifySecurityAnswer(answer: String): Boolean {
        val hashedInput = PinUtils.hashPin(answer.trim().lowercase())
        val storedHash = securityPrefs.hashedSecurityAnswerFlow.firstOrNull()
        return storedHash != null && hashedInput == storedHash
    }
    
    suspend fun resetPin(newPin: String) {
        securityPrefs.savePin(PinUtils.hashPin(newPin))
    }
    
    fun getNote(id: Int) = repository.getNote(id)
    
    fun saveNote(note: Note, onSaved: (Note) -> Unit = {}) {
        viewModelScope.launch {
            if (note.id == 0 || note.id == -1) {
                val newId = repository.insert(note)
                onSaved(note.copy(id = newId.toInt()))
            } else {
                repository.update(note)
                onSaved(note)
            }
        }
    }

    fun deleteNote(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPrefs.setAutoSaveEnabled(enabled)
        }
    }

    fun setAutoSaveInterval(seconds: Int) {
        viewModelScope.launch {
            securityPrefs.setAutoSaveInterval(seconds)
        }
    }

    fun setBiometricSimulationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            securityPrefs.setBiometricSimulationEnabled(enabled)
            // If simulation is turned off, also disable physical bio if we want to reset properly.
            if (!enabled) {
                securityPrefs.setBiometricEnabled(false)
            }
        }
    }

    fun nukeVault(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAll()
            securityPrefs.clearAll()
            onComplete()
        }
    }
}

class SharedViewModelFactory(
    private val repository: NoteRepository,
    private val securityPrefs: SecurityPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SharedViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SharedViewModel(repository, securityPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
