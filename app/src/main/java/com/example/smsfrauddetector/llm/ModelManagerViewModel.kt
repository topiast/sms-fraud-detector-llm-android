package com.example.smsfrauddetector.llm

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelManagerUiState(
    val models: List<Model> = emptyList(),
    val selectedModel: Model? = null,
    val modelDownloadStatus: Map<String, ModelDownloadStatus> = emptyMap(),
    val modelInitializationStatus: Map<String, Boolean> = emptyMap(),
    val showTokenDialog: Boolean = false
)

class ModelManagerViewModel(
    private val downloadRepository: DownloadRepository,
    private val context: Context,
    private val huggingFaceTokenManager: HuggingFaceTokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelManagerUiState())
    val uiState: StateFlow<ModelManagerUiState> = _uiState.asStateFlow()

    fun setModels(models: List<Model>) {
        _uiState.update { it.copy(models = models) }
        checkInitialDownloadStatus(models)
    }

    private fun checkInitialDownloadStatus(models: List<Model>) {
        viewModelScope.launch(Dispatchers.IO) {
            val newStatus = _uiState.value.modelDownloadStatus.toMutableMap()
            for (model in models) {
                val modelFile = java.io.File(model.getPath(context))
                android.util.Log.d("ModelDownloadStatusCheck", "Checking file: ${modelFile.absolutePath}, exists: ${modelFile.exists()}, size: ${if (modelFile.exists()) modelFile.length() else "N/A"}")
                val currentStatus = newStatus[model.name]?.status
                if (
                    (!newStatus.containsKey(model.name) || currentStatus == ModelDownloadStatusType.IDLE)
                    && modelFile.exists() && modelFile.length() == model.sizeInBytes
                ) {
                    android.util.Log.d("ModelDownloadStatusCheck", "Setting SUCCEEDED for ${model.name} after file check: exists=${modelFile.exists()}, size=${modelFile.length()}")
                    newStatus[model.name] = ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED)
                } else {
                    android.util.Log.d("ModelDownloadStatusCheck", "Not setting SUCCEEDED for ${model.name}: exists=${modelFile.exists()}, size=${if (modelFile.exists()) modelFile.length() else "N/A"}, currentStatus=${currentStatus}")
                }
            }
            _uiState.update { it.copy(modelDownloadStatus = newStatus) }
        }
    }

    fun selectModel(model: Model) {
        _uiState.update { it.copy(selectedModel = model) }
    }

    fun downloadModel(model: Model, lifecycleOwner: LifecycleOwner, huggingFaceToken: String? = null) {
        downloadRepository.downloadModel(model, lifecycleOwner, { updatedModel, status ->
            _uiState.update {
                val newStatus = it.modelDownloadStatus.toMutableMap()
                newStatus[updatedModel.name] = status
                it.copy(modelDownloadStatus = newStatus)
            }
            if (status.status == ModelDownloadStatusType.UNAUTHORIZED) {
                _uiState.update { it.copy(showTokenDialog = true) }
            }
        }, huggingFaceToken ?: huggingFaceTokenManager.getToken())
    }

    fun saveToken(token: String) {
        huggingFaceTokenManager.saveToken(token)
    }

    fun dismissTokenDialog() {
        _uiState.update { it.copy(showTokenDialog = false) }
    }

    fun initializeModel(model: Model) {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update {
                val newStatus = it.modelInitializationStatus.toMutableMap()
                newStatus[model.name] = true
                it.copy(modelInitializationStatus = newStatus)
            }
            LlmInferenceHelper.initialize(context, model) { errorMessage ->
                if (errorMessage.isEmpty()) {
                    _uiState.update {
                        val newStatus = it.modelInitializationStatus.toMutableMap()
                        newStatus[model.name] = false
                        it.copy(modelInitializationStatus = newStatus)
                    }
                } else {
                    _uiState.update {
                        val newStatus = it.modelInitializationStatus.toMutableMap()
                        newStatus[model.name] = false
                        it.copy(modelInitializationStatus = newStatus)
                    }
                }
            }
        }
    }

    fun cleanupModel(model: Model) {
        LlmInferenceHelper.cleanUp(model)
        _uiState.update {
            val newStatus = it.modelInitializationStatus.toMutableMap()
            newStatus.remove(model.name)
            it.copy(modelInitializationStatus = newStatus)
        }
    }
    fun deleteModel(model: Model) {
        viewModelScope.launch(Dispatchers.IO) {
            val status = _uiState.value.modelDownloadStatus[model.name]?.status
            // If downloading, cancel first
            if (status == ModelDownloadStatusType.DOWNLOADING) {
                downloadRepository.cancelDownloadModel(model)
            }
            // Delete the model file
            val modelFile = java.io.File(model.getPath(context))
            android.util.Log.d("ModelDelete", "Deleting file: ${modelFile.absolutePath}, exists: ${modelFile.exists()}, size: ${if (modelFile.exists()) modelFile.length() else "N/A"}")
            if (modelFile.exists()) {
                modelFile.delete()
                android.util.Log.d("ModelDelete", "File deleted: ${modelFile.absolutePath}")
            }
            // Update UI state to reset download status to IDLE
            _uiState.update {
                val newDownloadStatus = it.modelDownloadStatus.toMutableMap()
                newDownloadStatus[model.name] = ModelDownloadStatus(ModelDownloadStatusType.IDLE)
                it.copy(modelDownloadStatus = newDownloadStatus)
            }
        }
    }
}