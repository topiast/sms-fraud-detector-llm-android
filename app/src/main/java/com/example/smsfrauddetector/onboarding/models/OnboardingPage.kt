package com.example.smsfrauddetector.onboarding.models

data class OnboardingPage(
    val title: String,
    val description: String,
    val animationType: AnimationType,
    val interactiveDemo: InteractiveDemo?
)

enum class AnimationType {
    WELCOME_FADE_IN,
    AI_SCANNING_FLOW,
    NOTIFICATION_POPUP,
    PRIVACY_SHIELD,
    HISTORY_TIMELINE,
    PERMISSION_CHECKMARKS
}

sealed class InteractiveDemo {
    object MessageScanning : InteractiveDemo()
    object NotificationSamples : InteractiveDemo()
    object DataFlowVisualization : InteractiveDemo()
    object HistoryBrowsing : InteractiveDemo()
    object PermissionFlow : InteractiveDemo()
}