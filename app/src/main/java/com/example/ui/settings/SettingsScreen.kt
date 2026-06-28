package com.example.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SharedViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    sharedViewModel: SharedViewModel,
    onNavigateBack: () -> Unit
) {
    val isBioEnabled by sharedViewModel.securityPrefs.isBiometricEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
    val isBioSimulationEnabled by sharedViewModel.isBiometricSimulationEnabled.collectAsStateWithLifecycle(initialValue = false)
    val appTheme by sharedViewModel.securityPrefs.appThemeFlow.collectAsStateWithLifecycle(initialValue = "Cyber")
    val securityQuestion by sharedViewModel.securityQuestion.collectAsStateWithLifecycle(initialValue = null)
    
    var showSecurityQuestionDialog by remember { mutableStateOf(false) }
    var newSecurityQuestion by remember { mutableStateOf("") }
    var newSecurityAnswer by remember { mutableStateOf("") }
    var showManual by remember { mutableStateOf(false) }
    
    var showDecoyPinDialog by remember { mutableStateOf(false) }
    var decoyPinInput by remember { mutableStateOf("") }
    var decoyPinConfirmInput by remember { mutableStateOf("") }
    var decoyPinError by remember { mutableStateOf<String?>(null) }
    
    var showSelfDestructDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Manual Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App User Manual",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = "Learn how to use features & stay secure",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }
                }
                Switch(
                    checked = showManual,
                    onCheckedChange = { checked ->
                        showManual = checked
                    }
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))

            Text("Security", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            // 1. Biometric Simulation Sandbox Toggle
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Simulate Biometrics (Sandbox Mode)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = "Enable virtual fingerprint scanning dialog for testing and demo purposes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = isBioSimulationEnabled,
                    onCheckedChange = { checked ->
                        sharedViewModel.setBiometricSimulationEnabled(checked)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            // 2. Physical Fingerprint Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Fingerprint Unlock", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    val physicalAvailable = com.example.security.BiometricHelper.isBiometricAvailable(androidx.compose.ui.platform.LocalContext.current)
                    if (!physicalAvailable) {
                        if (isBioSimulationEnabled) {
                            Text(
                                text = "Physical hardware absent: Sandbox Simulation Active", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Text(
                                text = "Not available/enrolled on this device", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Text(
                            text = "Physical hardware detected and ready",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Switch(
                    checked = isBioEnabled,
                    enabled = isBioSimulationEnabled || com.example.security.BiometricHelper.isBiometricAvailable(androidx.compose.ui.platform.LocalContext.current),
                    onCheckedChange = { checked ->
                        scope.launch {
                            sharedViewModel.securityPrefs.setBiometricEnabled(checked)
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { 
                    newSecurityQuestion = securityQuestion ?: ""
                    newSecurityAnswer = ""
                    showSecurityQuestionDialog = true 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (securityQuestion != null) "Update Security Question" else "Set Security Question")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedButton(
                onClick = { 
                    decoyPinInput = ""
                    decoyPinConfirmInput = ""
                    decoyPinError = null
                    showDecoyPinDialog = true 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Configure Decoy / Fake PIN")
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Security Shield", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            val context = androidx.compose.ui.platform.LocalContext.current
            val isRooted = remember { com.example.security.SecurityHarden.isDeviceRooted() }
            val isDebugger = remember { com.example.security.SecurityHarden.isDebuggerAttached(context) }
            val isObfuscated = remember { com.example.security.SecurityHarden.isCodeObfuscated() }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isRooted) Icons.Default.Warning else Icons.Default.Lock,
                            contentDescription = "Security Status",
                            tint = if (isRooted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isRooted) "System Threat Detected" else "Device Environment Safe",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isRooted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Root Status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isRooted) "Rooted (Risk)" else "Secure (No Root)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (isRooted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Debugger Protection", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isDebugger) "Attached (Risk)" else "Secure",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (isDebugger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("R8 Code Obfuscation", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isObfuscated) "Active" else "Inactive",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Signature Storage Binding", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "AES-256 GCM Bound",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Nuclear Threat Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Emergency Self-Destruct",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Instantly wipe all stored notes, security settings, configuration logs, and encryption credentials from this device hardware.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showSelfDestructDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("NUKE ALL VAULT DATA", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Auto-Save", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            val isAutoSaveEnabled by sharedViewModel.isAutoSaveEnabled.collectAsStateWithLifecycle(initialValue = true)
            val autoSaveInterval by sharedViewModel.autoSaveInterval.collectAsStateWithLifecycle(initialValue = 5)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Save Notes", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = "Persist note progress to secure local storage automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = isAutoSaveEnabled,
                    onCheckedChange = { checked ->
                        sharedViewModel.setAutoSaveEnabled(checked)
                    }
                )
            }
            
            if (isAutoSaveEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Auto-Save Interval",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val intervals = listOf(3, 5, 10, 30)
                    intervals.forEach { secs ->
                        val isSelected = autoSaveInterval == secs
                        FilterChip(
                            selected = isSelected,
                            onClick = { sharedViewModel.setAutoSaveInterval(secs) },
                            label = { Text("${secs}s") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Appearance", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = appTheme == "Cyber",
                    onClick = {
                        scope.launch { sharedViewModel.securityPrefs.setAppTheme("Cyber") }
                    }
                )
                Text("Cyber Dark", color = MaterialTheme.colorScheme.onBackground)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = appTheme == "BrightDark",
                    onClick = {
                        scope.launch { sharedViewModel.securityPrefs.setAppTheme("BrightDark") }
                    }
                )
                Text("Bright Dark", color = MaterialTheme.colorScheme.onBackground)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "This app created by Arpan AKA Devil\n(creator of DCL)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            )
        }
        
        if (showManual) {
            AlertDialog(
                onDismissRequest = { showManual = false },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App User Manual", style = MaterialTheme.typography.titleLarge)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Welcome to the Secret Notes Pro Manual. This application is engineered with ultimate hardware-bound security layers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text("1. Dual Vault System", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "The app features two distinct spaces:\n" +
                            "• Real Vault: Access your genuine secret notes using your primary PIN.\n" +
                            "• Decoy Vault: Set a Fake PIN during setup. Entering the Fake PIN unlocks a completely independent decoy screen to mislead unauthorized lookers.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text("2. Biometric Protection", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Enable fingerprint unlock inside Settings to seamlessly authenticate without manually typing your passcode every time.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text("3. Hardware & Signature Encrypted Storage", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "All notes are stored in a SQLite Room Database encrypted with robust AES-256 GCM. The encryption keys are securely bound to the device's Keystore, package name, and unique signing signature.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text("4. System Shield (Environment Monitor)", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "The app monitors execution context and alerts you if root access, active debuggers, or emulator runtimes are detected.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { showManual = false }) {
                        Text("Got It")
                    }
                }
            )
        }
        
        if (showSecurityQuestionDialog) {
            AlertDialog(
                onDismissRequest = { showSecurityQuestionDialog = false },
                title = { Text("Security Question") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newSecurityQuestion,
                            onValueChange = { newSecurityQuestion = it },
                            label = { Text("Question") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newSecurityAnswer,
                            onValueChange = { newSecurityAnswer = it },
                            label = { Text("Answer") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newSecurityQuestion.isNotBlank() && newSecurityAnswer.isNotBlank()) {
                                scope.launch {
                                    val hashedAnswer = com.example.security.PinUtils.hashPin(newSecurityAnswer.trim().lowercase())
                                    sharedViewModel.securityPrefs.saveSecurityQuestion(
                                        newSecurityQuestion.trim(), 
                                        hashedAnswer
                                    )
                                    showSecurityQuestionDialog = false
                                }
                            }
                        },
                        enabled = newSecurityQuestion.isNotBlank() && newSecurityAnswer.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSecurityQuestionDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDecoyPinDialog) {
            AlertDialog(
                onDismissRequest = { showDecoyPinDialog = false },
                title = { Text("Set Up Decoy Vault PIN") },
                text = {
                    Column {
                        Text(
                            text = "Entering this Fake PIN during lockscreen unlock loads an empty decoy notes screen, protecting your real secrets under duress.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        OutlinedTextField(
                            value = decoyPinInput,
                            onValueChange = { 
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    decoyPinInput = it
                                }
                            },
                            label = { Text("New 4-Digit Decoy PIN") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = decoyPinConfirmInput,
                            onValueChange = { 
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    decoyPinConfirmInput = it
                                }
                            },
                            label = { Text("Confirm Decoy PIN") },
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        decoyPinError?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (decoyPinInput.length != 4) {
                                decoyPinError = "PIN must be exactly 4 digits."
                            } else if (decoyPinInput != decoyPinConfirmInput) {
                                decoyPinError = "PINs do not match."
                            } else {
                                scope.launch {
                                    val hashedFake = com.example.security.PinUtils.hashPin(decoyPinInput)
                                    sharedViewModel.securityPrefs.saveFakePin(hashedFake)
                                    showDecoyPinDialog = false
                                }
                            }
                        },
                        enabled = decoyPinInput.isNotEmpty() && decoyPinConfirmInput.isNotEmpty()
                    ) {
                        Text("Save Decoy PIN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDecoyPinDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showSelfDestructDialog) {
            AlertDialog(
                onDismissRequest = { showSelfDestructDialog = false },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("NUCLEAR WIPE VAULT?", color = MaterialTheme.colorScheme.error)
                    }
                },
                text = {
                    Text("This is an irreversible hardware and storage self-destruct command.\n\nAll notes, keys, decoy PINs, security logs, and settings will be permanently erased. The application will instantly exit and reboot fresh.\n\nAre you absolutely sure?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showSelfDestructDialog = false
                            sharedViewModel.nukeVault {
                                // Wipe completely completed. Terminate immediately.
                                android.os.Process.killProcess(android.os.Process.myPid())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("YES, WIPE FOREVER")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSelfDestructDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}
