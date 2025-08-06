package com.example.smsfrauddetector.llm

import android.content.Context
import java.io.File

data class Model(
    val name: String,
    val version: String = "1.0",
    val downloadFileName: String,
    val url: String,
    val sizeInBytes: Long,
    val estimatedPeakMemoryInBytes: Long = 0,
    val info: String = "",
    val learnMoreUrl: String = "",
    val llmSupportImage: Boolean = false,
    val showBenchmarkButton: Boolean = true,
    val showRunAgainButton: Boolean = true,
    val configs: List<Config> = listOf(),
    var instance: Any? = null, // To hold the loaded model instance
    var initializing: Boolean = false,
    var configValues: Map<String, Any> = mapOf()
) {
    val normalizedName: String = name.replace(Regex("[^a-zA-Z0-9]"), "_")

    fun getPath(context: Context): String {
        val baseDir = listOf(
            context.getExternalFilesDir(null)?.absolutePath ?: "",
            normalizedName,
            version
        ).joinToString(File.separator)
        return "$baseDir/$downloadFileName"
    }

    fun preProcess() {
        val configMap: MutableMap<String, Any> = mutableMapOf()
        for (config in this.configs) {
            configMap[config.key.label] = config.defaultValue
        }
        this.configValues = configMap
    }
}
object ModelRegistry {
    val models = listOf(
        Model(
            name = "Gemma-3n-E2B-it-int4",
            version = "20250520",
            downloadFileName = "gemma-3n-E2B-it-int4.task",
            url = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task?download=true",
            sizeInBytes = 3136226711L,
            estimatedPeakMemoryInBytes = 5905580032L,
            info = "Preview version of Gemma 3n E2B ready for deployment on Android using the MediaPipe LLM Inference API. The current checkpoint only supports text and vision input, with 4096 context length.",
            learnMoreUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview",
            llmSupportImage = true,
            configs = createLlmChatConfigs(
                defaultTopK = 64,
                defaultTopP = 0.95f,
                defaultTemperature = 1f,
                defaultMaxToken = 4096,
                accelerators = listOf(Accelerator.CPU, Accelerator.GPU)
            ),
            showBenchmarkButton = false,
            showRunAgainButton = false
        ),
        Model(
            name = "Gemma-3n-E4B-it-int4",
            version = "20250520",
            downloadFileName = "gemma-3n-E4B-it-int4.task",
            url = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview/resolve/main/gemma-3n-E4B-it-int4.task?download=true",
            sizeInBytes = 4405655031L,
            estimatedPeakMemoryInBytes = 6979321856L,
            info = "Preview version of Gemma 3n E4B ready for deployment on Android using the MediaPipe LLM Inference API. The current checkpoint only supports text and vision input, with 4096 context length.",
            learnMoreUrl = "https://huggingface.co/google/gemma-3n-E4B-it-litert-preview",
            llmSupportImage = true,
            configs = createLlmChatConfigs(
                defaultTopK = 64,
                defaultTopP = 0.95f,
                defaultTemperature = 1f,
                defaultMaxToken = 4096,
                accelerators = listOf(Accelerator.CPU, Accelerator.GPU)
            ),
            showBenchmarkButton = false,
            showRunAgainButton = false
        )
    )
}