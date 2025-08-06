package com.example.smsfrauddetector

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.smsfrauddetector.database.SmsReport
import com.example.smsfrauddetector.database.SmsReportRepository
import com.example.smsfrauddetector.ui.theme.SmsFraudDetectorTheme
import kotlinx.coroutines.launch

class HistoryActivity : ComponentActivity() {
    
    private lateinit var repository: SmsReportRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        repository = SmsReportRepository.getInstance(this)
        
        setContent {
            SmsFraudDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HistoryScreen(
                        repository = repository,
                        modifier = Modifier.padding(innerPadding),
                        onBackPressed = { finish() },
                        onReportClicked = { reportId ->
                            val intent = Intent(this@HistoryActivity, SummaryActivity::class.java)
                            intent.putExtra("REPORT_ID", reportId)
                            startActivity(intent)
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    repository: SmsReportRepository,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    onReportClicked: (String) -> Unit
) {
    var reports by remember { mutableStateOf<List<SmsReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf(FilterType.ALL) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Collect reports from repository
    LaunchedEffect(selectedFilter) {
        isLoading = true
        try {
            when (selectedFilter) {
                FilterType.ALL -> {
                    repository.getAllReports().collect {
                        android.util.Log.d("HistoryActivity", "Loaded reports: $it")
                        reports = it
                        isLoading = false
                    }
                }
                FilterType.FRAUD -> {
                    repository.getFraudReports().collect {
                        android.util.Log.d("HistoryActivity", "Loaded fraud reports: $it")
                        reports = it
                        isLoading = false
                    }
                }
                FilterType.SAFE -> {
                    repository.getSafeReports().collect {
                        android.util.Log.d("HistoryActivity", "Loaded safe reports: $it")
                        reports = it
                        isLoading = false
                    }
                }
                FilterType.FLAGGED -> {
                    repository.getFlaggedReports().collect {
                        android.util.Log.d("HistoryActivity", "Loaded flagged reports: $it")
                        reports = it
                        isLoading = false
                    }
                }
            }
        } catch (e: Exception) {
            // Handle error
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message History") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Chips
            FilterChipsRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it }
        )
        
        // Reports List
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (reports.isEmpty()) {
            EmptyStateScreen(filterType = selectedFilter)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reports, key = { it.id }) { report ->
                    SwipeableReportListItem(
                        modifier = Modifier.animateItemPlacement(),
                        report = report,
                        onClick = { onReportClicked(report.id) },
                        onToggleFlag = {
                            scope.launch {
                                try {
                                    val currentReport = repository.getReportById(report.id)
                                    if (currentReport != null) {
                                        val newFlagStatus = !currentReport.isManuallyFlagged
                                        repository.updateManualFlag(report.id, newFlagStatus)
                                        Toast.makeText(context, if (newFlagStatus) "Message flagged" else "Flag removed", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to update flag", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDeleteReport = {
                            scope.launch {
                                try {
                                    repository.deleteReport(report.id)
                                    Toast.makeText(context, "Report deleted", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to delete report", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterType.values().forEach { filter ->
            FilterChip(
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
                selected = selectedFilter == filter,
                // Removed weight to prevent text wrapping
                // modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableReportListItem(
    modifier: Modifier = Modifier,
    report: SmsReport,
    onClick: () -> Unit,
    onToggleFlag: () -> Unit,
    onDeleteReport: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            when (it) {
                SwipeToDismissBoxValue.EndToStart -> { // Swipe left to flag
                    onToggleFlag()
                    false // Snap back
                }
                SwipeToDismissBoxValue.StartToEnd -> { // Swipe right to delete
                    showDeleteDialog = true
                    false // Snap back, show dialog
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Report") },
            text = { Text("Are you sure you want to delete this report? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteReport()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.padding(vertical = 1.dp), // To show the rounded corners
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }, label = "background color"
            )

            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.CenterStart // Default
            }

            val icon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                SwipeToDismissBoxValue.EndToStart -> if (report.isManuallyFlagged) Icons.Filled.Flag else Icons.Outlined.Flag
                else -> null
            }
            
            val iconTint = when(direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onErrorContainer
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> Color.Transparent
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                icon?.let {
                    Icon(
                        it,
                        contentDescription = "Action",
                        tint = iconTint
                    )
                }
            }
        }
    ) {
        ReportListItem(
            report = report,
            onClick = onClick
        )
    }
}

@Composable
fun ReportListItem(
    report: SmsReport,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = when {
                report.isFraud -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Processing status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (report.processingStatus) {
                    "processing" -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFF59D) // Yellow
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFFFBC02D)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Processing…",
                                    fontSize = 12.sp,
                                    color = Color(0xFFFBC02D),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    "stopped" -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Stopped",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    "processed" -> {
                         Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE0E0E0)
                        ) {
                            Text(
                                text = "Processed",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                color = Color(0xFF388E3C),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Row (left side) gets weight to take all available space
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (report.processingStatus == "processed") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (report.isFraud) "⚠️" else "✅",
                                fontSize = 20.sp
                            )
                            Text(
                                text = if (report.isFraud) "POSSIBLE SCAM" else "LIKELY SAFE",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (report.isFraud)
                                    MaterialTheme.colorScheme.error.copy(alpha = 1.0f)
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Analysis pending...",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Flagged/percentage Row (right side) never wraps
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(start = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (report.isManuallyFlagged) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary
                        ) {
                            Text(
                                text = "🚩",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        text = "${report.getFraudPercentage()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
            
            Text(
                text = "From: ${report.sender ?: "Unknown"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = report.messageBody,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = report.getFormattedDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStateScreen(filterType: FilterType) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📭",
                fontSize = 48.sp
            )
            Text(
                text = when (filterType) {
                    FilterType.ALL -> "No messages processed yet"
                    FilterType.FRAUD -> "No fraud messages detected"
                    FilterType.SAFE -> "No safe messages found"
                    FilterType.FLAGGED -> "No flagged messages"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = when (filterType) {
                    FilterType.ALL -> "SMS messages will appear here after being processed"
                    FilterType.FRAUD -> "Potentially fraudulent messages will appear here"
                    FilterType.SAFE -> "Safe messages will appear here"
                    FilterType.FLAGGED -> "Manually flagged messages will appear here"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class FilterType(val displayName: String) {
    ALL("All"),
    FRAUD("Fraud"),
    SAFE("Safe"),
    FLAGGED("Flagged")
}