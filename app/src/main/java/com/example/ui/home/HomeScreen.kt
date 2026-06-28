package com.example.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Note
import com.example.ui.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedViewModel: SharedViewModel,
    onNavigateToNote: (Int) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val notes by sharedViewModel.allNotes.collectAsStateWithLifecycle()
    val isFakeVault by sharedViewModel.isFakeVaultActive.collectAsStateWithLifecycle()
    
    // Security states for locked note accessing
    var pendingNoteIdToUnlock by remember { mutableStateOf<Int?>(null) }
    var showSimulatedBiometricPrompt by remember { mutableStateOf(false) }
    var showPinPromptForUnlock by remember { mutableStateOf(false) }
    
    val isBioEnabled by sharedViewModel.securityPrefs.isBiometricEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val isBioSimulationEnabled by sharedViewModel.isBiometricSimulationEnabled.collectAsStateWithLifecycle(initialValue = false)
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val triggerUnlockFlow = { noteId: Int ->
        pendingNoteIdToUnlock = noteId
        if (isBioEnabled) {
            val physicalAvailable = com.example.security.BiometricHelper.isBiometricAvailable(context)
            if (physicalAvailable) {
                val activity = context as? androidx.fragment.app.FragmentActivity
                if (activity != null) {
                    com.example.security.BiometricHelper.showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            onNavigateToNote(noteId)
                            pendingNoteIdToUnlock = null
                        },
                        onFailed = {
                            showPinPromptForUnlock = true
                        },
                        onError = {
                            showPinPromptForUnlock = true
                        }
                    )
                } else {
                    showPinPromptForUnlock = true
                }
            } else if (isBioSimulationEnabled) {
                showSimulatedBiometricPrompt = true
            } else {
                showPinPromptForUnlock = true
            }
        } else {
            showPinPromptForUnlock = true
        }
    }

    // In fake vault mode, we filter notes or just show empty. For simplicity, we just show empty list.
    val displayNotes = if (isFakeVault) emptyList() else notes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isFakeVault) "Fake Vault" else "Secret Notes") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToNote(-1) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, "Add Note")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (displayNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No notes found. Tap + to add.", color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayNotes, key = { it.id }) { note ->
                    NoteCard(note = note, onClick = { 
                        if (note.isLocked) {
                            triggerUnlockFlow(note.id)
                        } else {
                            onNavigateToNote(note.id)
                        }
                    })
                }
            }
        }
    }

    // Simulated Biometric Prompt Dialog
    if (showSimulatedBiometricPrompt) {
        com.example.ui.components.SimulatedBiometricPrompt(
            onSuccess = {
                showSimulatedBiometricPrompt = false
                pendingNoteIdToUnlock?.let { onNavigateToNote(it) }
                pendingNoteIdToUnlock = null
            },
            onDismiss = {
                showSimulatedBiometricPrompt = false
                pendingNoteIdToUnlock = null
            },
            onFallbackPin = {
                showSimulatedBiometricPrompt = false
                showPinPromptForUnlock = true
            }
        )
    }

    // PIN fallback dialog for unlocking
    if (showPinPromptForUnlock) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { 
                showPinPromptForUnlock = false
                pendingNoteIdToUnlock = null
            },
            title = { Text("Secure PIN Required") },
            text = {
                Column {
                    Text("Enter your 4-6 digit vault PIN to unlock this protected note.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it },
                        label = { Text("Vault PIN") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        ),
                        isError = pinError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text(
                            text = "Incorrect secure PIN",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = sharedViewModel.verifyPin(pinInput)
                            if (success) {
                                showPinPromptForUnlock = false
                                pendingNoteIdToUnlock?.let { onNavigateToNote(it) }
                                pendingNoteIdToUnlock = null
                            } else {
                                pinError = true
                                pinInput = ""
                            }
                        }
                    }
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPinPromptForUnlock = false
                        pendingNoteIdToUnlock = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NoteCard(note: Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (note.isLocked) "Protected Note" else note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (note.isLocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (!note.isLocked) {
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            Text(
                text = sdf.format(Date(note.modifiedDate)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
