package com.example.ui.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.Note
import com.example.ui.SharedViewModel
import com.example.ui.components.SecureTextEditor
import com.example.utils.ExportUtils
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    noteId: Int,
    sharedViewModel: SharedViewModel,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLocked by remember { mutableStateOf(false) }
    var currentNote by remember { mutableStateOf<Note?>(null) }
    
    var showSaveAnimation by remember { mutableStateOf(false) }
    var showLockAnimation by remember { mutableStateOf(false) }
    var lastLockState by remember { mutableStateOf(false) }
    
    val isAutoSaveEnabled by sharedViewModel.isAutoSaveEnabled.collectAsState()
    val autoSaveInterval by sharedViewModel.autoSaveInterval.collectAsState()
    var isSavingSilently by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }

    val txtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let { ExportUtils.exportToTxt(context, it, title, content) }
    }
    val htmlLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri ->
        uri?.let { ExportUtils.exportToHtml(context, it, title, content) }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        uri?.let { ExportUtils.exportToPdf(context, it, title, content) }
    }
    
    LaunchedEffect(isAutoSaveEnabled, autoSaveInterval) {
        if (isAutoSaveEnabled) {
            while (true) {
                kotlinx.coroutines.delay(autoSaveInterval * 1000L)
                val finalTitle = title.trim()
                val finalContent = content.trim()
                val hasChanges = (currentNote == null && (finalTitle.isNotEmpty() || finalContent.isNotEmpty())) ||
                        (currentNote != null && (finalTitle != currentNote?.title || finalContent != currentNote?.content))
                
                if (hasChanges) {
                    isSavingSilently = true
                    val finalNote = currentNote?.update(finalTitle, finalContent, newIsLocked = isLocked) 
                        ?: Note.create(finalTitle, finalContent, isLocked = isLocked)
                    sharedViewModel.saveNote(finalNote) { savedNote ->
                        currentNote = savedNote
                        scope.launch {
                            kotlinx.coroutines.delay(1000)
                            isSavingSilently = false
                        }
                    }
                }
            }
        }
    }
    
    LaunchedEffect(noteId) {
        if (noteId != -1) {
            val note = sharedViewModel.getNote(noteId).firstOrNull()
            note?.let {
                currentNote = it
                title = it.title
                content = it.content
                isLocked = it.isLocked
                lastLockState = it.isLocked
            }
        }
    }

    LaunchedEffect(isLocked) {
        if (isLocked != lastLockState) {
            showLockAnimation = true
            lastLockState = isLocked
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isAutoSaveEnabled) {
                                Icon(
                                    imageVector = if (isSavingSilently) Icons.Default.Save else Icons.Default.Check,
                                    contentDescription = "Auto-save status",
                                    tint = if (isSavingSilently) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isSavingSilently) "Saving securely..." else "Saved securely",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSavingSilently) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color(0xFF4CAF50).copy(alpha = 0.8f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            val finalNote = currentNote?.update(title, content, newIsLocked = isLocked) 
                                ?: Note.create(title, content, isLocked = isLocked)
                            if (title.isNotEmpty() || content.isNotEmpty()) {
                                sharedViewModel.saveNote(finalNote)
                            }
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isLocked = !isLocked }) {
                            Icon(
                                imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                contentDescription = "Lock Toggle",
                                tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box {
                            IconButton(onClick = { showShareMenu = true }) {
                                Icon(Icons.Filled.Share, "Share Note", tint = MaterialTheme.colorScheme.primary)
                            }
                            DropdownMenu(
                                expanded = showShareMenu,
                                onDismissRequest = { showShareMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Share via Apps") },
                                    onClick = { 
                                        showShareMenu = false
                                        ExportUtils.shareText(context, title, content)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Save as PDF") },
                                    onClick = { 
                                        showShareMenu = false
                                        val fileName = if (title.isBlank()) "Note.pdf" else "$title.pdf"
                                        pdfLauncher.launch(fileName)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Save as TXT") },
                                    onClick = { 
                                        showShareMenu = false
                                        val fileName = if (title.isBlank()) "Note.txt" else "$title.txt"
                                        txtLauncher.launch(fileName)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Save as HTML") },
                                    onClick = { 
                                        showShareMenu = false
                                        val fileName = if (title.isBlank()) "Note.html" else "$title.html"
                                        htmlLauncher.launch(fileName)
                                    }
                                )
                            }
                        }
                        if (noteId != -1) {
                            IconButton(onClick = {
                                sharedViewModel.deleteNote(noteId)
                                onNavigateBack()
                            }) {
                                Icon(Icons.Filled.Delete, "Delete Note", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        val finalNote = currentNote?.update(title, content, newIsLocked = isLocked) 
                            ?: Note.create(title, content, isLocked = isLocked)
                        if (title.isNotEmpty() || content.isNotEmpty()) {
                            sharedViewModel.saveNote(finalNote) { savedNote ->
                                currentNote = savedNote
                                showSaveAnimation = true
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Save, contentDescription = "Save Note") },
                    text = { Text("Save Note") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            SecureTextEditor(
                title = title,
                onTitleChange = { title = it },
                content = content,
                onContentChange = { content = it },
                isLocked = isLocked,
                onLockToggle = { isLocked = !isLocked },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            )
        }

        if (showSaveAnimation) {
            SecureSaveAnimationOverlay(onFinished = { showSaveAnimation = false })
        }

        if (showLockAnimation) {
            SecureLockAnimationOverlay(isLocked = isLocked, onFinished = { showLockAnimation = false })
        }
    }
}

@Composable
fun SecureSaveAnimationOverlay(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1200)
        onFinished()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val scaleProgress by animateFloatAsState(
                    targetValue = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseScale"
                )
                
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer(scaleX = scaleProgress, scaleY = scaleProgress)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "AES-256 ENCRYPTED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                    color = Color(0xFF4CAF50)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Note stored securely in Devil Vault",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SecureLockAnimationOverlay(isLocked: Boolean, onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        onFinished()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val scale by animateFloatAsState(
                    targetValue = 1.1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy),
                    label = "LockBounce"
                )
                
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Lock Animation",
                    tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = if (isLocked) "NOTE LOCKED SECURELY" else "NOTE UNLOCKED",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
