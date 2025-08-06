package com.example.smsfrauddetector.llm

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.work.*
import java.util.UUID

interface DownloadRepository {
    fun downloadModel(model: Model, lifecycleOwner: LifecycleOwner, onStatusUpdated: (Model, ModelDownloadStatus) -> Unit, huggingFaceToken: String? = null)
    fun cancelDownloadModel(model: Model)
}

class DefaultDownloadRepository(
    private val context: Context
) : DownloadRepository {
    private val workManager = WorkManager.getInstance(context)

    override fun downloadModel(model: Model, lifecycleOwner: LifecycleOwner, onStatusUpdated: (Model, ModelDownloadStatus) -> Unit, huggingFaceToken: String?) {
        val inputDataBuilder = Data.Builder()
            .putString(DownloadWorker.KEY_MODEL_URL, model.url)
            .putString(DownloadWorker.KEY_MODEL_NAME, model.name)
            .putString(DownloadWorker.KEY_MODEL_DOWNLOAD_FILE_NAME, model.downloadFileName)
            .putString(DownloadWorker.KEY_MODEL_DOWNLOAD_MODEL_DIR, model.normalizedName)
            .putString(DownloadWorker.KEY_MODEL_VERSION, model.version)


        huggingFaceToken?.let {
            inputDataBuilder.putString(DownloadWorker.KEY_HUGGING_FACE_TOKEN, it)
        }

        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputDataBuilder.build())
            .build()

        workManager.enqueueUniqueWork(model.name, ExistingWorkPolicy.REPLACE, downloadWorkRequest)
        observerWorkerProgress(downloadWorkRequest.id, model, lifecycleOwner, onStatusUpdated)
    }

    override fun cancelDownloadModel(model: Model) {
        workManager.cancelAllWorkByTag(model.name)
    }

    private fun observerWorkerProgress(workerId: UUID, model: Model, lifecycleOwner: LifecycleOwner, onStatusUpdated: (Model, ModelDownloadStatus) -> Unit) {
        workManager.getWorkInfoByIdLiveData(workerId).observe(lifecycleOwner) { workInfo ->
            when (workInfo?.state) {
                WorkInfo.State.ENQUEUED -> {
                    onStatusUpdated(model, ModelDownloadStatus(ModelDownloadStatusType.IDLE))
                }
                WorkInfo.State.RUNNING -> {
                    val progress = workInfo.progress.getInt(DownloadWorker.KEY_DOWNLOAD_PROGRESS, 0)
                    onStatusUpdated(model, ModelDownloadStatus(ModelDownloadStatusType.DOWNLOADING, progress.toFloat() / 100))
                }
                WorkInfo.State.SUCCEEDED -> {
                    val modelFile = java.io.File(model.getPath(context))
                    android.util.Log.d("ModelDownload", "Download finished for: ${modelFile.absolutePath}, exists: ${modelFile.exists()}, size: ${if (modelFile.exists()) modelFile.length() else "N/A"}")
                    onStatusUpdated(model, ModelDownloadStatus(ModelDownloadStatusType.SUCCEEDED))
                }
                WorkInfo.State.FAILED -> {
                    val error = workInfo.outputData.getString(DownloadWorker.KEY_DOWNLOAD_ERROR)
                    if (error == "Unauthorized") {
                        onStatusUpdated(model, ModelDownloadStatus(ModelDownloadStatusType.UNAUTHORIZED, error = error))
                    } else {
                        onStatusUpdated(model, ModelDownloadStatus(ModelDownloadStatusType.FAILED, error = error))
                    }
                }
                WorkInfo.State.CANCELLED -> {
                    onStatusUpdated(model, ModelDownloadStatus(ModelDownloadStatusType.CANCELED))
                }
                else -> {
                    // Do nothing
                }
            }
        }
    }
}