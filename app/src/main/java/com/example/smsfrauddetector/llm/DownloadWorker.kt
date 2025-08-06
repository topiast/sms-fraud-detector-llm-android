package com.example.smsfrauddetector.llm

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_MODEL_URL = "KEY_MODEL_URL"
        const val KEY_MODEL_NAME = "KEY_MODEL_NAME"
        const val KEY_MODEL_DOWNLOAD_FILE_NAME = "KEY_MODEL_DOWNLOAD_FILE_NAME"
        const val KEY_MODEL_DOWNLOAD_MODEL_DIR = "KEY_MODEL_DOWNLOAD_MODEL_DIR"
        const val KEY_MODEL_VERSION = "KEY_MODEL_VERSION"
        const val KEY_DOWNLOAD_PROGRESS = "KEY_DOWNLOAD_PROGRESS"
        const val KEY_DOWNLOAD_ERROR = "KEY_DOWNLOAD_ERROR"
        const val KEY_HUGGING_FACE_TOKEN = "KEY_HUGGING_FACE_TOKEN"
    }

    override suspend fun doWork(): Result {
        val modelUrl = inputData.getString(KEY_MODEL_URL) ?: return Result.failure()
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: return Result.failure()
        val downloadFileName = inputData.getString(KEY_MODEL_DOWNLOAD_FILE_NAME) ?: return Result.failure()
        val modelDir = inputData.getString(KEY_MODEL_DOWNLOAD_MODEL_DIR) ?: return Result.failure()
        val modelVersion = inputData.getString(KEY_MODEL_VERSION) ?: return Result.failure()
        val huggingFaceToken = inputData.getString(KEY_HUGGING_FACE_TOKEN)

        val outputDir = File(context.getExternalFilesDir(null), "$modelDir/$modelVersion")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val outputFile = File(outputDir, downloadFileName)

        return try {
            val url = URL(modelUrl)
            val connection = url.openConnection() as HttpURLConnection
            huggingFaceToken?.let {
                connection.setRequestProperty("Authorization", "Bearer $it")
            }
            connection.connect()
            Log.d("DownloadWorker", "Response code: ${connection.responseCode}")

            if (connection.responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                return Result.failure(workDataOf(KEY_DOWNLOAD_ERROR to "Unauthorized"))
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return Result.failure(workDataOf(KEY_DOWNLOAD_ERROR to "Server returned HTTP ${connection.responseCode} ${connection.responseMessage}"))
            }

            val fileLength = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(outputFile)

            val data = ByteArray(4096)
            var total: Long = 0
            var count: Int
            while (input.read(data).also { count = it } != -1) {
                if (isStopped) {
                    output.close()
                    input.close()
                    connection.disconnect()
                    return Result.failure(workDataOf(KEY_DOWNLOAD_ERROR to "Download cancelled"))
                }
                total += count.toLong()
                if (fileLength > 0) {
                    val progress = (total * 100 / fileLength).toInt()
                    setProgress(workDataOf(KEY_DOWNLOAD_PROGRESS to progress))
                }
                output.write(data, 0, count)
            }
            output.close()
            input.close()
            connection.disconnect()
            Result.success()
        } catch (e: Exception) {
            Log.e("DownloadWorker", "Error downloading model", e)
            Result.failure(workDataOf(KEY_DOWNLOAD_ERROR to e.message))
        }
    }
}