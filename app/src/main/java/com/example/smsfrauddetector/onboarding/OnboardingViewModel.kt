package com.example.smsfrauddetector.onboarding

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.smsfrauddetector.onboarding.models.*

class OnboardingViewModel : ViewModel() {
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()
    
    val pages = listOf(
        OnboardingPage(
            title = "Welcome to SMS Fraud Detector",
            description = "Your private guardian against SMS fraud. All detection happens locally on your device.",
            animationType = AnimationType.WELCOME_FADE_IN,
            interactiveDemo = null
        ),
        OnboardingPage(
            title = "AI-Powered Detection",
            description = "Advanced AI analyzes your messages using rules and machine learning - all processing stays on your device.",
            animationType = AnimationType.AI_SCANNING_FLOW,
            interactiveDemo = InteractiveDemo.MessageScanning
        ),
        OnboardingPage(
            title = "Manual Analysis",
            description = "Manually check suspicious texts by sharing them to the app for instant analysis.",
            animationType = AnimationType.NOTIFICATION_POPUP,
            interactiveDemo = InteractiveDemo.NotificationSamples
        ),
        OnboardingPage(
            title = "History",
            description = "View analysis history of all messages.",
            animationType = AnimationType.HISTORY_TIMELINE,
            interactiveDemo = InteractiveDemo.HistoryBrowsing
        ),
        OnboardingPage(
            title = "Permissions",
            description = "Grant the necessary permissions to enable SMS fraud detection and notifications.",
            animationType = AnimationType.PRIVACY_SHIELD,
            interactiveDemo = InteractiveDemo.DataFlowVisualization
        )
    )
    // Removed the last (empty) onboarding page to prevent an empty view at the end.
    
    fun nextPage() {
        if (_currentPage.value < pages.size - 1) {
            _currentPage.value += 1
            _isLastPage.value = _currentPage.value == pages.size - 1
        }
    }
    
    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value -= 1
            _isLastPage.value = false
        }
    }
    
    fun skipToEnd() {
        _currentPage.value = pages.size - 1
        _isLastPage.value = true
    }
    
    fun getCurrentPage(): OnboardingPage {
        return pages[_currentPage.value]
    }
}