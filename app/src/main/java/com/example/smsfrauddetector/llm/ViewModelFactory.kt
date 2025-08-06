package com.example.smsfrauddetector.llm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ModelManagerViewModel::class.java)) {
            val downloadRepository = DefaultDownloadRepository(context)
            val huggingFaceTokenManager = HuggingFaceTokenManager(context)
            @Suppress("UNCHECKED_CAST")
            return ModelManagerViewModel(downloadRepository, context, huggingFaceTokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}