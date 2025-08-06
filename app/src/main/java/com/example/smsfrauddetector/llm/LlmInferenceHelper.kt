package com.example.smsfrauddetector.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference

object LlmInferenceHelper {

    fun initialize(context: Context, model: Model, onDone: (String) -> Unit) {
        Log.d("LlmInferenceHelper", "Initializing model: ${model.name}")
        try {
            Log.d("LlmInferenceHelper", "Setting model path: ${model.getPath(context)}")
            val builder = LlmInference.LlmInferenceOptions.builder()
            Log.d("LlmInferenceHelper", "Setting max tokens: ${model.configValues[ConfigKey.MAX_TOKENS.label]}")
            builder.setMaxTokens(model.configValues[ConfigKey.MAX_TOKENS.label] as Int)
            Log.d("LlmInferenceHelper", "Building options")
            val options = builder
                .setModelPath(model.getPath(context))
                .build()

            Log.d("LlmInferenceHelper", "Creating LlmInference instance")
            val llmInference = LlmInference.createFromOptions(context, options)
            model.instance = llmInference
            Log.d("LlmInferenceHelper", "Model initialized successfully")
            onDone("")
        } catch (e: Exception) {
            Log.e("LlmInferenceHelper", "Error initializing model: ${e.message}")
            onDone(e.message ?: "Unknown error during initialization")
        }
    }

    fun generateResponse(
        model: Model,
        prompt: String
    ): String {
        Log.d("LlmInferenceHelper", "Generating response for prompt: $prompt")
        val llmInference = model.instance as LlmInference
        try {
            val response = llmInference.generateResponse(prompt)
            Log.d("LlmInferenceHelper", "Generated response: $response")
            return response
        } catch (e: Exception) {
            Log.e("LlmInferenceHelper", "Error generating response: ${e.message}")
            return "Error: ${e.message}"
        }
    }

    fun cleanUp(model: Model) {
        if (model.instance != null) {
            (model.instance as LlmInference).close()
            model.instance = null
        }
    }
}