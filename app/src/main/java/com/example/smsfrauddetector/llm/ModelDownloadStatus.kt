package com.example.smsfrauddetector.llm

enum class ModelDownloadStatusType {
    IDLE,
    DOWNLOADING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    UNAUTHORIZED
}

data class ModelDownloadStatus(
    val status: ModelDownloadStatusType,
    val progress: Float = 0f,
    val error: String? = null
)