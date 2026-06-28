package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.security.PinUtils
import com.example.ui.SharedViewModel
import kotlinx.coroutines.launch

@Composable
fun PinSetupScreen(
    sharedViewModel: SharedViewModel,
    onSetupComplete: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var securityQuestion by remember { mutableStateOf("") }
    var securityAnswer by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isFakeSetupMode by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isFakeSetupMode) "Set up Fake Vault PIN" else "Set up your secure PIN",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 6) pin = it },
            label = { Text("Enter PIN (4-6 digits)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { if (it.length <= 6) confirmPin = it },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            isError = isError
        )
        
        if (!isFakeSetupMode) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = securityQuestion,
                onValueChange = { securityQuestion = it },
                label = { Text("Security Question (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = securityAnswer,
                onValueChange = { securityAnswer = it },
                label = { Text("Answer") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        if (isError) {
            Text(errorMessage.ifEmpty { "PINs do not match or are too short" }, color = MaterialTheme.colorScheme.error)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = {
                isError = false
                if (pin.length < 4 || pin != confirmPin) {
                    isError = true
                    errorMessage = "PINs do not match or are too short"
                    return@Button
                }
                if (!isFakeSetupMode && securityQuestion.isNotBlank() && securityAnswer.isBlank()) {
                    isError = true
                    errorMessage = "Please provide an answer to your security question"
                    return@Button
                }
                if (!isFakeSetupMode && securityQuestion.isBlank() && securityAnswer.isNotBlank()) {
                    isError = true
                    errorMessage = "Please provide a security question"
                    return@Button
                }
                
                val hashed = PinUtils.hashPin(pin)
                scope.launch {
                    if (isFakeSetupMode) {
                        sharedViewModel.securityPrefs.saveFakePin(hashed)
                        onSetupComplete()
                    } else {
                        sharedViewModel.securityPrefs.savePin(hashed)
                        if (securityQuestion.isNotBlank() && securityAnswer.isNotBlank()) {
                            val hashedAnswer = PinUtils.hashPin(securityAnswer.trim().lowercase())
                            sharedViewModel.securityPrefs.saveSecurityQuestion(securityQuestion.trim(), hashedAnswer)
                        }
                        isFakeSetupMode = true
                        pin = ""
                        confirmPin = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isFakeSetupMode) "Finish Setup" else "Next")
        }
        
        if (isFakeSetupMode) {
            TextButton(onClick = { onSetupComplete() }) {
                Text("Skip Fake Vault Setup")
            }
        }
    }
}
