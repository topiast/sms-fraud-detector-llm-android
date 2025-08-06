package com.example.smsfrauddetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.smsfrauddetector.SettingsManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsfrauddetector.llm.Accelerator
import com.example.smsfrauddetector.llm.DefaultDownloadRepository
import com.example.smsfrauddetector.llm.Model
import com.example.smsfrauddetector.llm.ModelDownloadStatus
import com.example.smsfrauddetector.llm.ModelDownloadStatusType
import com.example.smsfrauddetector.llm.ModelManagerViewModel
import com.example.smsfrauddetector.llm.ViewModelFactory
import com.example.smsfrauddetector.llm.createLlmChatConfigs
import com.example.smsfrauddetector.ui.theme.SmsFraudDetectorTheme

class ModelDownloadActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmsFraudDetectorTheme {
                ModelDownloadScreen(lifecycleOwner = this)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDownloadScreen(lifecycleOwner: LifecycleOwner) {
    val context = LocalContext.current as ComponentActivity
    val modelManagerViewModel: ModelManagerViewModel = viewModel(factory = ViewModelFactory(context))

    val models = com.example.smsfrauddetector.llm.ModelRegistry.models
    modelManagerViewModel.setModels(models)

    val uiState by modelManagerViewModel.uiState.collectAsState()

    // Use a state variable with a trigger to force recomposition when persistent storage changes
    var recompositionTrigger by remember { mutableStateOf(0) }
    val selectedModelName by remember {
        derivedStateOf {
            // This will re-evaluate when recompositionTrigger changes
            recompositionTrigger // Read the trigger to create dependency
            SettingsManager.getSelectedModelName(context)
        }
    }

    // Keep track of initialization state to prevent unnecessary operations
    var isInitialized by remember { mutableStateOf(false) }

    // Helper function to trigger recomposition after settings change
    fun triggerRecomposition() {
        recompositionTrigger++
        android.util.Log.d("ModelDownload", "Triggered recomposition: $recompositionTrigger")
    }

    // Handle lifecycle events to refresh state when returning to the activity
    val lifecycleOwner2 = LocalContext.current as? ComponentActivity
    DisposableEffect(lifecycleOwner2) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    android.util.Log.d("ModelDownload", "Lifecycle ON_RESUME: Current selectedModelName: $selectedModelName")
                    // Trigger recomposition to pick up any external changes
                    triggerRecomposition()
                }
                else -> { /* No action needed */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Log current state for debugging
    LaunchedEffect(selectedModelName) {
        android.util.Log.d("ModelDownload", "Current selectedModelName from SettingsManager: $selectedModelName")
    }

    if (uiState.showTokenDialog) {
        var token by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { modelManagerViewModel.dismissTokenDialog() },
            title = { Text("Hugging Face Token") },
            text = {
                TextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Token") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        modelManagerViewModel.saveToken(token)
                        uiState.selectedModel?.let {
                            modelManagerViewModel.downloadModel(it, lifecycleOwner, token)
                        }
                        modelManagerViewModel.dismissTokenDialog()
                    }
                ) {
                    Text("Save and Retry")
                }
            },
            dismissButton = {
                Button(onClick = { modelManagerViewModel.dismissTokenDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Auto-selection logic - only run when models and download status are fully loaded
    LaunchedEffect(uiState.models, uiState.modelDownloadStatus, isInitialized) {
        // Wait for all model statuses to be loaded
        val allStatusLoaded = uiState.models.all { uiState.modelDownloadStatus.containsKey(it.name) }
        if (!allStatusLoaded) {
            android.util.Log.d("ModelDownload", "Auto-select logic: Not all model statuses loaded yet, skipping auto-select")
            return@LaunchedEffect
        }

        // Mark as initialized after first load
        if (!isInitialized) {
            isInitialized = true
        }

        val downloadedModels = uiState.models.filter {
            uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
        }

        val currentSelection = SettingsManager.getSelectedModelName(context)
        val currentIsAvailable = downloadedModels.any { it.name == currentSelection }

        android.util.Log.d("ModelDownload", "Auto-select logic: currentSelection=$currentSelection, currentIsAvailable=$currentIsAvailable, downloadedModels=${downloadedModels.map { it.name }}")

        // Only auto-select if current selection is invalid or null
        if (currentSelection == null || !currentIsAvailable) {
            when {
                downloadedModels.size == 1 -> {
                    val onlyModel = downloadedModels.first()
                    android.util.Log.d("ModelDownload", "Auto-selecting only downloaded model: ${onlyModel.name}")
                    SettingsManager.setSelectedModelName(context, onlyModel.name)
                }
                downloadedModels.size > 1 && currentSelection == null -> {
                    // If multiple models available and no selection, pick the first one
                    val firstModel = downloadedModels.first()
                    android.util.Log.d("ModelDownload", "Auto-selecting first of multiple models: ${firstModel.name}")
                    SettingsManager.setSelectedModelName(context, firstModel.name)
                }
                downloadedModels.isEmpty() -> {
                    android.util.Log.d("ModelDownload", "No downloaded models, clearing selection")
                    SettingsManager.setSelectedModelName(context, null)
                }
            }
        } else {
            android.util.Log.d("ModelDownload", "Current selection '$currentSelection' is valid, keeping it")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Download Models") },
                navigationIcon = {
                    IconButton(onClick = { context.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(uiState.models) { model ->
                    val isDownloaded = uiState.modelDownloadStatus[model.name]?.status == ModelDownloadStatusType.SUCCEEDED
                    val progress = uiState.modelDownloadStatus[model.name]?.progress ?: 0f
                    ModelDownloadItem(
                        model = model,
                        downloadStatus = uiState.modelDownloadStatus[model.name],
                        isDefault = selectedModelName == model.name,
                        canSetDefault = isDownloaded,
                        downloadProgress = progress,
                        onSetDefault = {
                            android.util.Log.d("ModelDownload", "Manual selection: Set selectedModelName to ${model.name}")
                            SettingsManager.setSelectedModelName(context, model.name)
                            triggerRecomposition()
                        },
                        onDownload = {
                            modelManagerViewModel.selectModel(model)
                            modelManagerViewModel.downloadModel(model, lifecycleOwner)
                        },
                        onDelete = {
                            android.util.Log.d("ModelDownload", "Deleting model: ${model.name}")
                            modelManagerViewModel.deleteModel(model)

                            // Handle selection after deletion
                            if (selectedModelName == model.name) {
                                val remainingDownloaded = uiState.models.filter {
                                    it.name != model.name &&
                                            uiState.modelDownloadStatus[it.name]?.status == ModelDownloadStatusType.SUCCEEDED
                                }

                                val newSelection = remainingDownloaded.firstOrNull()
                                android.util.Log.d("ModelDownload", "Auto-selecting after deletion: ${newSelection?.name}")
                                SettingsManager.setSelectedModelName(context, newSelection?.name)
                                triggerRecomposition()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModelDownloadItem(
    model: Model,
    downloadStatus: ModelDownloadStatus?,
    isDefault: Boolean = false,
    canSetDefault: Boolean = false,
    onSetDefault: () -> Unit = {},
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    downloadProgress: Float = 0f
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDownloaded = downloadStatus?.status == ModelDownloadStatusType.SUCCEEDED
    val isDownloading = downloadStatus?.status == ModelDownloadStatusType.DOWNLOADING

    // Enhanced visual styling based on state
    val borderStroke = if (isDefault) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    } else {
        null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = borderStroke,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Model info section
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Size: %.2f MB".format(model.sizeInBytes / 1024.0 / 1024.0),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Status badges
                    if (isDownloaded || isDefault) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isDownloaded) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ) {
                                    Text(
                                        text = "Downloaded",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            if (isDefault) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(
                                        text = "Default",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons section
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (downloadStatus?.status) {
                        ModelDownloadStatusType.SUCCEEDED -> {
                            // Show contextual actions for downloaded models
                            if (!isDefault && canSetDefault) {
                                Button(
                                    onClick = onSetDefault,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Text("Set Default")
                                }
                            }

                            Button(
                                onClick = { showDeleteDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Delete")
                            }
                        }

                        ModelDownloadStatusType.DOWNLOADING -> {
                            // Show progress during download
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Downloading...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                LinearProgressIndicator(
                                    progress = downloadProgress,
                                    modifier = Modifier.width(120.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${(downloadProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        ModelDownloadStatusType.FAILED -> {
                            Button(
                                onClick = onDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Retry")
                            }
                        }

                        ModelDownloadStatusType.UNAUTHORIZED -> {
                            Button(
                                onClick = onDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text("Auth Required")
                            }
                        }

                        ModelDownloadStatusType.IDLE, null -> {
                            Button(
                                onClick = onDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Download")
                            }
                        }

                        else -> {
                            Button(
                                onClick = onDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Download")
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Model",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${model.name}\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}