package com.example.smsfrauddetector.onboarding.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingNavigation(
    currentPage: Int,
    totalPages: Int,
    isLastPage: Boolean,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onSkipClick: () -> Unit,
    onGetStartedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip/Previous Button
        if (currentPage == 0) {
            TextButton(
                onClick = onSkipClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            TextButton(
                onClick = onPreviousClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Previous",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Page Indicator
        PageIndicator(
            currentPage = currentPage,
            totalPages = totalPages,
            modifier = Modifier.weight(1f)
        )
        
        // Next/Get Started Button
        if (isLastPage) {
            TextButton(
                onClick = onGetStartedClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Start",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            TextButton(
                onClick = onNextClick,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Next",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}