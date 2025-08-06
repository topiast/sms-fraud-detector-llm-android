package com.example.smsfrauddetector.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.example.smsfrauddetector.MainActivity
import com.example.smsfrauddetector.SettingsManager
import com.example.smsfrauddetector.onboarding.components.OnboardingNavigation
import com.example.smsfrauddetector.onboarding.screens.*
import com.example.smsfrauddetector.ui.theme.SmsFraudDetectorTheme
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {
    private val viewModel: OnboardingViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            SmsFraudDetectorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        onComplete = {
                            completeOnboarding()
                        }
                    )
                }
            }
        }
    }
    
    private fun completeOnboarding() {
        SettingsManager.setOnboardingCompleted(this)
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    val currentPage by viewModel.currentPage.collectAsState()
    val isLastPage by viewModel.isLastPage.collectAsState()
    val pagerState = rememberPagerState(pageCount = { viewModel.pages.size })
    val scope = rememberCoroutineScope()
    var skipInProgress by remember { mutableStateOf(false) }

    // Sync viewmodel with pager state
    LaunchedEffect(currentPage) {
        pagerState.animateScrollToPage(currentPage)
        if (skipInProgress) {
            skipInProgress = false
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (skipInProgress) return@LaunchedEffect
        if (pagerState.currentPage != currentPage) {
            // Update viewmodel when user swipes
            repeat(kotlin.math.abs(pagerState.currentPage - currentPage)) {
                if (pagerState.currentPage > currentPage) {
                    viewModel.nextPage()
                } else {
                    viewModel.previousPage()
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main content with HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { it }
            ) { page ->
                when (page) {
                    0 -> WelcomeScreen()
                    1 -> AIDetectionScreen()
                    2 -> ManualAnalysisScreen()
                    3 -> HistoryScreen()
                    4 -> PermissionsScreen()
                }
            }
            
            // Navigation controls
            OnboardingNavigation(
                currentPage = currentPage,
                totalPages = viewModel.pages.size,
                isLastPage = isLastPage,
                onPreviousClick = {
                    scope.launch {
                        viewModel.previousPage()
                    }
                },
                onNextClick = {
                    scope.launch {
                        viewModel.nextPage()
                    }
                },
                onSkipClick = {
                    scope.launch {
                        skipInProgress = true
                        viewModel.skipToEnd()
                    }
                },
                onGetStartedClick = onComplete
            )
        }
    }
}