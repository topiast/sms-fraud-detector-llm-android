package com.example.smsfrauddetector.onboarding.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class HistoryItem(
    val message: String,
    val sender: String,
    val time: String,
    val isFraud: Boolean,
    val isManualCheck: Boolean = false
)

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier
) {
    var visibleItems by remember { mutableStateOf(0) }
    // Manual analysis UI removed
    
    val historyItems = listOf(
        HistoryItem("URGENT! Your account will be suspended...", "Unknown", "2 min ago", true),
        HistoryItem("Your bank verification code is 123456", "BANK", "1 hour ago", false),
        HistoryItem("Click here to claim your prize now!", "PROMO", "3 hours ago", true),
        HistoryItem("Appointment confirmed for tomorrow", "Dr. Smith", "Yesterday", false),
        // Manual analysis item removed
    )
    
    LaunchedEffect(Unit) {
        while (true) {
            for (i in 0..historyItems.size) {
                visibleItems = i
                delay(800)
            }
            delay(1000)
            visibleItems = 0
            delay(1000)
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
            text = "Message History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "View analysis history of all messages.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // History demo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Message History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Animated history list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(historyItems.take(visibleItems)) { index, item ->
                        androidx.compose.animation.AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = tween(300)
                            ) + fadeIn()
                        ) {
                            HistoryItemCard(item = item)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        // Feature card and section removed as requested
    }
}

@Composable
private fun HistoryItemCard(
    item: HistoryItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isFraud) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Status icon
            Icon(
                imageVector = if (item.isFraud) Icons.Default.Warning else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (item.isFraud) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.sender,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Manual check badge removed
            }
        }
    }
}

