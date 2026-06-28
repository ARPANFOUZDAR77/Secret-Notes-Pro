package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SimulatedBiometricPrompt(
    onSuccess: () -> Unit,
    onDismiss: () -> Unit,
    onFallbackPin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scanState by remember { mutableStateOf("IDLE") } // IDLE, SCANNING, SUCCESS, FAILED
    var scanProgress by remember { mutableStateOf(0f) }
    var attemptsRemaining by remember { mutableStateOf(3) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Error shake animation offset
    val shakeOffset = remember { Animatable(0f) }

    // Pulsing outline animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutLinearInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Handle touch scanning progress
    LaunchedEffect(scanState) {
        if (scanState == "SCANNING") {
            scanProgress = 0f
            while (scanProgress < 1.0f && scanState == "SCANNING") {
                delay(30)
                scanProgress += 0.04f
            }
            if (scanState == "SCANNING") {
                // Determine success or random fail for testing (90% success)
                if (Math.random() < 0.9 || attemptsRemaining <= 1) {
                    scanState = "SUCCESS"
                    delay(800)
                    onSuccess()
                } else {
                    scanState = "FAILED"
                    attemptsRemaining -= 1
                    // Play shake animation
                    scope.launch {
                        shakeOffset.animateTo(
                            targetValue = 15f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
                        )
                        shakeOffset.animateTo(
                            targetValue = -15f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
                        )
                        shakeOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                        )
                    }
                    delay(2000)
                    scanState = "IDLE"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(
                    width = 1.dp,
                    color = when (scanState) {
                        "SUCCESS" -> Color(0xFF4CAF50).copy(alpha = 0.6f)
                        "FAILED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        "SCANNING" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(24.dp)
                )
                .graphicsLayer(translationX = shakeOffset.value)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Panel
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "BIOMETRIC VAULT LOCK",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Confirm Identity to Access",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Touch and hold the secure scanner below or use your device credential fallback.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // The Visual Biometric Fingerprint Sensor Touch Area
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(140.dp)
                        .pointerInput(attemptsRemaining) {
                            if (attemptsRemaining > 0 && scanState != "SUCCESS") {
                                detectTapGestures(
                                    onPress = {
                                        if (scanState == "IDLE" || scanState == "FAILED") {
                                            scanState = "SCANNING"
                                            tryAwaitRelease()
                                            if (scanState == "SCANNING") {
                                                scanState = "IDLE"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    // Outer Ring
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .graphicsLayer(
                                scaleX = if (scanState == "SCANNING") pulseScale else 1.0f,
                                scaleY = if (scanState == "SCANNING") pulseScale else 1.0f
                            )
                            .border(
                                width = 3.dp,
                                color = when (scanState) {
                                    "SUCCESS" -> Color(0xFF4CAF50)
                                    "FAILED" -> MaterialTheme.colorScheme.error
                                    "SCANNING" -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                },
                                shape = CircleShape
                            )
                    )

                    // Progress Loader Ring
                    if (scanState == "SCANNING") {
                        CircularProgressIndicator(
                            progress = { scanProgress },
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            modifier = Modifier.size(130.dp)
                        )
                    }

                    // Scan Laser Line
                    if (scanState == "SCANNING") {
                        val scanTransition = rememberInfiniteTransition(label = "Laser")
                        val laserOffset by scanTransition.animateFloat(
                            initialValue = -50f,
                            targetValue = 50f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "LaserMotion"
                        )
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(2.dp)
                                .graphicsLayer(translationY = laserOffset)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Fingerprint Icon Panel
                    Surface(
                        shape = CircleShape,
                        color = when (scanState) {
                            "SUCCESS" -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                            "FAILED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            "SCANNING" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AnimatedContent(
                                targetState = scanState,
                                transitionSpec = {
                                    scaleIn() togetherWith scaleOut()
                                },
                                label = "FingerprintIcon"
                            ) { state ->
                                when (state) {
                                    "SUCCESS" -> Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    "FAILED" -> Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Failed",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    else -> Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "Fingerprint Sensor",
                                        tint = if (state == "SCANNING") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Display Status Msg
                Text(
                    text = when (scanState) {
                        "SCANNING" -> "Scanning Biometric: ${(scanProgress * 100).toInt()}%"
                        "SUCCESS" -> "Access Granted"
                        "FAILED" -> "No Match! Try Again"
                        else -> "HOLD FINGER TO SENSOR"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (scanState) {
                        "SUCCESS" -> Color(0xFF4CAF50)
                        "FAILED" -> MaterialTheme.colorScheme.error
                        "SCANNING" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )

                if (attemptsRemaining < 3 && attemptsRemaining > 0) {
                    Text(
                        text = "Attempts remaining: $attemptsRemaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                } else if (attemptsRemaining == 0) {
                    Text(
                        text = "Scanner Lockout: Please use PIN fallback",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Footer Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onFallbackPin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Use PIN Backup", fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}
