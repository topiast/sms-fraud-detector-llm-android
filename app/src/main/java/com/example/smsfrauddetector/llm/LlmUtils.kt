package com.example.smsfrauddetector.llm

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

suspend fun loadAndProcessLlmMessage(
    context: Context,
    model: Model,
    prompt: String,
    systemPrompt: String? = null
): String {
    val fullPrompt = if (systemPrompt.isNullOrEmpty()) {
        prompt
    } else {
        "$systemPrompt\n\n$prompt"
    }

    return suspendCancellableCoroutine { continuation ->
        LlmInferenceHelper.initialize(context, model) { error ->
            if (error.isNotEmpty()) {
                continuation.resume("Error initializing model: $error")
                return@initialize
            }

            // Run blocking inference directly (should be called from IO context)
            val response = LlmInferenceHelper.generateResponse(model, fullPrompt)
            LlmInferenceHelper.cleanUp(model)
            continuation.resume(response)
        }
    }
}
enum class Accelerator {
    CPU,
    GPU,
    NNAPI
}

fun createLlmChatConfigs(
    defaultTopK: Int,
    defaultTopP: Float,
    defaultTemperature: Float,
    defaultMaxToken: Int,
    accelerators: List<Accelerator>
): List<Config> {
    return listOf(
        NumberSliderConfig(
            key = ConfigKey.TOPK,
            sliderMin = 1f,
            sliderMax = 128f,
            defaultValue = defaultTopK,
            valueType = ValueType.INT
        ),
        NumberSliderConfig(
            key = ConfigKey.TOPP,
            sliderMin = 0f,
            sliderMax = 1f,
            defaultValue = defaultTopP,
            valueType = ValueType.FLOAT
        ),
        NumberSliderConfig(
            key = ConfigKey.TEMPERATURE,
            sliderMin = 0f,
            sliderMax = 1f,
            defaultValue = defaultTemperature,
            valueType = ValueType.FLOAT
        ),
        NumberSliderConfig(
            key = ConfigKey.MAX_TOKENS,
            sliderMin = 1f,
            sliderMax = 8192f,
            defaultValue = defaultMaxToken,
            valueType = ValueType.INT
        ),
        DropDownConfig(
            key = ConfigKey.ACCELERATOR,
            defaultValue = accelerators.first().name,
            options = accelerators.map { it.name }
        )
    )
}