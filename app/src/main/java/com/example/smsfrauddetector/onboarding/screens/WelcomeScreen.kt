package com.example.smsfrauddetector.onboarding.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.example.smsfrauddetector.R

@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(1000),
        label = "alpha"
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Logo/Icon
        Card(
            modifier = Modifier
                .size(120.dp)
                .scale(scale),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_onboarding_magnifier_check),
                    contentDescription = "SMS Fraud Detector Logo",
                    modifier = Modifier.size(80.dp),
                    tint = Color.Unspecified
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Animated Title
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(800, delayMillis = 300)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 300))
        ) {
            Text(
                text = "Welcome to\nSMS Fraud Detector",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Animated Description
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(800, delayMillis = 600)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 600))
        ) {
            Text(
                text = "Your private guardian against SMS fraud.\nAll detection happens locally on your device.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Animated Privacy Badge
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it / 2 },
                animationSpec = tween(800, delayMillis = 900)
            ) + fadeIn(animationSpec = tween(800, delayMillis = 900))
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Privacy",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "100% Private & Secure",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}