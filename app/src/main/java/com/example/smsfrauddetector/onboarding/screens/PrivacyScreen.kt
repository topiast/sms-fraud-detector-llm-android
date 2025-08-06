package com.example.smsfrauddetector.onboarding.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// --- FIX: Added the missing imports here ---
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import com.example.smsfrauddetector.R
// --- END FIX ---
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun ManualAnalysisScreen(
    modifier: Modifier = Modifier
) {
    var animationStep by remember { mutableStateOf(0) }

    // This effect runs the animation loop
    LaunchedEffect(Unit) {
        while (true) {
            animationStep = 0 // Initial state
            delay(1500)
            animationStep = 1 // Text selected, 3-dots appear
            delay(1500)
            animationStep = 2 // Context menu with 'Share' appears
            delay(1500)
            animationStep = 3 // Share sheet slides in
            delay(2000)
            animationStep = 4 // App selected in share sheet
            delay(2500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = "Manual Analysis with Android Share",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "You can manually analyze any message or email by using Android's native Share feature.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Animation area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp), // Increased height for more space
            contentAlignment = Alignment.Center
        ) {
            AnimationContent(animationStep)
        }

        Spacer(modifier = Modifier.height(32.dp))

    //     // Instructions Card
    //     Card(
    //         modifier = Modifier.fillMaxWidth(),
    //         colors = CardDefaults.cardColors(
    //             containerColor = MaterialTheme.colorScheme.primaryContainer
    //         ),
    //         shape = RoundedCornerShape(16.dp)
    //     ) {
    //         Row(
    //             modifier = Modifier
    //                 .fillMaxWidth()
    //                 .padding(24.dp),
    //             verticalAlignment = Alignment.CenterVertically
    //         ) {
    //             Icon(
    //                 imageVector = Icons.Default.Share,
    //                 contentDescription = "Share",
    //                 tint = MaterialTheme.colorScheme.primary,
    //                 modifier = Modifier.size(40.dp)
    //             )
    //             Spacer(modifier = Modifier.width(24.dp))
    //             Column {
    //                 Text(
    //                     text = "How to use:",
    //                     style = MaterialTheme.typography.titleMedium,
    //                     fontWeight = FontWeight.Bold,
    //                     color = MaterialTheme.colorScheme.onPrimaryContainer
    //                 )
    //                 Spacer(modifier = Modifier.height(8.dp))
    //                 Text(
    //                     text = "1. Long-press to select text in any app\n" +
    //                             "2. Tap the 'More' (⋮) or 'Share' button\n" +
    //                             "3. Choose SMS Fraud Detector to analyze",
    //                     style = MaterialTheme.typography.bodyMedium,
    //                     color = MaterialTheme.colorScheme.onPrimaryContainer
    //                 )
    //             }
    //         }
    //     }
    }
}

@Composable
private fun AnimationContent(step: Int) {
    val textBgColor by animateColorAsState(
        targetValue = if (step >= 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent,
        animationSpec = tween(500),
        label = "TextBgColorAnimation"
    )

    // A Box to stack the message, context menu, and share sheet
    Box(contentAlignment = Alignment.TopCenter) {
        // Mock message UI
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp) // Pushed down to make space for menu
        ) {
            // Mock message bubble
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Click here to claim your prize!",
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(textBgColor)
                        .padding(4.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        // The "three dots" icon and the context menu that appears from it
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-8).dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Three dots icon
            AnimatedVisibility(
                visible = step == 1,
                enter = fadeIn(animationSpec = tween(300)) + scaleIn(),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Context Menu that appears after clicking the three dots
            AnimatedVisibility(
                visible = step == 2,
                enter = fadeIn() + scaleIn(transformOrigin = TransformOrigin(0.5f, 0f)),
                exit = fadeOut() + scaleOut(transformOrigin = TransformOrigin(0.5f, 0f))
            ) {
                ContextMenu()
            }
        }

        // Share Sheet that slides up from the bottom
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = step >= 3,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ShareSheet(isSelected = step == 4)
            }
        }
    }
}

@Composable
private fun ContextMenu() {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Text("Copy", modifier = Modifier.fillMaxWidth().padding(16.dp))
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ShareSheet(isSelected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F7FA)) // Soft light background for share sheet
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Share with...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AppIcon(name = "Messages")
                AppIcon(name = "Gmail")
                SmsDetectorAppIcon(isSelected = isSelected)
                AppIcon(name = "Copy")
            }
        }
    }
}

@Composable
private fun AppIcon(name: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) { /* Placeholder for real app icons */ }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = name, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SmsDetectorAppIcon(isSelected: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SmsDetectorIconScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFE3F2FD) else Color.White,
        animationSpec = tween(400),
        label = "SmsDetectorIconBgColor"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
        animationSpec = tween(400),
        label = "SmsDetectorIconTint"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_onboarding_magnifier_check),
                contentDescription = "SMS Fraud Detector",
                tint = iconTint,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "SMS Fraud\nDetector",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}