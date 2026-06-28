package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.animation.core.*
import androidx.fragment.app.FragmentActivity
import com.example.security.BiometricHelper
import com.example.ui.SharedViewModel
import com.example.ui.components.SimulatedBiometricPrompt
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    activity: FragmentActivity,
    sharedViewModel: SharedViewModel,
    onAuthSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var remainingAttempts by remember { mutableStateOf(5) }
    var isUnlockingHype by remember { mutableStateOf(false) }
    
    var showSimulatedBiometricPrompt by remember { mutableStateOf(false) }
    
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var resetSecurityAnswer by remember { mutableStateOf("") }
    var resetError by remember { mutableStateOf(false) }
    
    var showNewPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var newPinError by remember { mutableStateOf("") }
    
    val scope = rememberCoroutineScope()
    
    val isBioEnabled by sharedViewModel.securityPrefs.isBiometricEnabledFlow.collectAsState(initial = false)
    val isBioSimulationEnabled by sharedViewModel.isBiometricSimulationEnabled.collectAsState(initial = false)
    val securityQuestion by sharedViewModel.securityQuestion.collectAsState()

    LaunchedEffect(isBioEnabled, isBioSimulationEnabled) {
        if (isBioEnabled) {
            if (BiometricHelper.isBiometricAvailable(activity)) {
                BiometricHelper.showBiometricPrompt(
                    activity = activity,
                    onSuccess = { onAuthSuccess() },
                    onFailed = { /* Do nothing, they can try PIN */ },
                    onError = { /* Ignore */ }
                )
            } else if (isBioSimulationEnabled) {
                showSimulatedBiometricPrompt = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Fingerprint,
            contentDescription = "Vault",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp).padding(bottom = 16.dp)
        )
        
        Text(
            text = "Unlock Vault",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("Enter PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            isError = isError
        )
        
        if (isError) {
            Text(
                text = "Incorrect PIN. Attempts remaining: $remainingAttempts", 
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                scope.launch {
                    val success = sharedViewModel.verifyPin(pin)
                    if (success) {
                        isUnlockingHype = true
                    } else {
                        isError = true
                        remainingAttempts -= 1
                        pin = ""
                        if (remainingAttempts <= 0) {
                            // In a real app, block for 30s. For now just reset to 5
                            remainingAttempts = 5
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Unlock")
        }
        
        if (isBioEnabled && (isBioSimulationEnabled || BiometricHelper.isBiometricAvailable(activity))) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    if (BiometricHelper.isBiometricAvailable(activity)) {
                        BiometricHelper.showBiometricPrompt(
                            activity = activity,
                            onSuccess = { isUnlockingHype = true },
                            onFailed = {},
                            onError = {}
                        )
                    } else {
                        showSimulatedBiometricPrompt = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use Biometrics")
            }
        }
        
        if (securityQuestion != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { showForgotPinDialog = true }) {
                Text("Forgot PIN?")
            }
        }
    }
    
    if (showSimulatedBiometricPrompt) {
        SimulatedBiometricPrompt(
            onSuccess = {
                showSimulatedBiometricPrompt = false
                isUnlockingHype = true
            },
            onDismiss = {
                showSimulatedBiometricPrompt = false
            },
            onFallbackPin = {
                showSimulatedBiometricPrompt = false
            }
        )
    }

    if (isUnlockingHype) {
        SecureUnlockHypeOverlay(
            onFinished = {
                isUnlockingHype = false
                onAuthSuccess()
            }
        )
    }
    
    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showForgotPinDialog = false
                resetError = false
                resetSecurityAnswer = ""
            },
            title = { Text("Security Question") },
            text = {
                Column {
                    Text(securityQuestion ?: "")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resetSecurityAnswer,
                        onValueChange = { resetSecurityAnswer = it },
                        label = { Text("Answer") },
                        isError = resetError
                    )
                    if (resetError) {
                        Text("Incorrect answer", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        if (sharedViewModel.verifySecurityAnswer(resetSecurityAnswer)) {
                            showForgotPinDialog = false
                            resetError = false
                            resetSecurityAnswer = ""
                            showNewPinDialog = true
                        } else {
                            resetError = true
                        }
                    }
                }) {
                    Text("Verify")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showForgotPinDialog = false
                    resetError = false
                    resetSecurityAnswer = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    if (showNewPinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNewPinDialog = false
                newPin = ""
                confirmNewPin = ""
                newPinError = ""
            },
            title = { Text("Reset PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6) newPin = it },
                        label = { Text("New PIN (4-6 digits)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmNewPin,
                        onValueChange = { if (it.length <= 6) confirmNewPin = it },
                        label = { Text("Confirm New PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (newPinError.isNotEmpty()) {
                        Text(newPinError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPin.length < 4 || newPin != confirmNewPin) {
                        newPinError = "PINs do not match or are too short"
                    } else {
                        scope.launch {
                            sharedViewModel.resetPin(newPin)
                            showNewPinDialog = false
                            newPin = ""
                            confirmNewPin = ""
                            newPinError = ""
                            onAuthSuccess() // Automatically unlock after resetting
                        }
                    }
                }) {
                    Text("Reset & Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNewPinDialog = false
                    newPin = ""
                    confirmNewPin = ""
                    newPinError = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SecureUnlockHypeOverlay(onFinished: () -> Unit) {
    var currentStage by remember { mutableStateOf(1) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(400)
        currentStage = 2
        kotlinx.coroutines.delay(400)
        currentStage = 3
        kotlinx.coroutines.delay(400)
        currentStage = 4
        kotlinx.coroutines.delay(600)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "HypeRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    val scaleProgress by animateFloatAsState(
        targetValue = if (currentStage == 4) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "LockScale"
    )

    val stageText = when (currentStage) {
        1 -> "VERIFYING CRYPTO SIGNATURES..."
        2 -> "DECODING SECURE KEYSTORE KEY..."
        3 -> "DECRYPTING NOTES SQLITE STORE..."
        else -> "ACCESS GRANTED! WELCOME TO DEVIL VAULT"
    }

    val textColor = if (currentStage == 4) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                // Outer cybernetic dash ring
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = rotationAngle)
                ) {
                    val radius = size.minDimension / 2
                    val strokeWidth = 4.dp.toPx()
                    drawCircle(
                        color = textColor.copy(alpha = 0.25f),
                        radius = radius - strokeWidth,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(40f, 20f), 0f
                            )
                        )
                    )
                }
                
                // Inner solid pulse circle
                val innerPulseScale by animateFloatAsState(
                    targetValue = if (currentStage == 4) 1.15f else 1.0f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "InnerPulse"
                )
                
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (currentStage == 4) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer(scaleX = innerPulseScale, scaleY = innerPulseScale),
                    tonalElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (currentStage == 4) {
                                Icons.Default.LockOpen
                            } else {
                                Icons.Default.Lock
                            },
                            contentDescription = "Lock State",
                            tint = if (currentStage == 4) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier
                                .size(54.dp)
                                .graphicsLayer(scaleX = scaleProgress, scaleY = scaleProgress)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Progress text with neon glow look using shadow
            Text(
                text = stageText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                ),
                color = textColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = {
                    when (currentStage) {
                        1 -> 0.25f
                        2 -> 0.5f
                        3 -> 0.75f
                        else -> 1f
                    }
                },
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape),
                color = if (currentStage == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
