package com.example.smsfrauddetector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.smsfrauddetector.SettingsManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.smsfrauddetector.ui.theme.SmsFraudDetectorTheme

class MainActivity : ComponentActivity() {

    private var hasSmsPermission by mutableStateOf(false)
    private var hasNotificationPermission by mutableStateOf(false)
    private var detectionEnabled by mutableStateOf(true)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermission = permissions[Manifest.permission.READ_SMS] == true &&
                permissions[Manifest.permission.RECEIVE_SMS] == true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = permissions[Manifest.permission.POST_NOTIFICATIONS] == true
        } else {
            hasNotificationPermission = true
        }
    }

    private fun setSmsDetectionEnabled(enabled: Boolean) {
        detectionEnabled = enabled
        // Persist the setting
        SettingsManager.setDetectionEnabled(this, enabled)
        val pm = packageManager
        val componentName = android.content.ComponentName(this, SmsReceiver::class.java)
        val newState = if (enabled)
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        pm.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
        android.util.Log.i("MainActivity", "SMS detection " + if (enabled) "ENABLED" else "DISABLED")
        if (!enabled) {
            val intent = Intent(this, FraudDetectionService::class.java).apply {
                action = FraudDetectionService.ACTION_STOP_ALL_PROCESSING
            }
            startService(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if onboarding is completed
        if (!SettingsManager.isOnboardingCompleted(this)) {
            val intent = Intent(this, com.example.smsfrauddetector.onboarding.OnboardingActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        checkPermissions()

        // Load persisted detectionEnabled value
        detectionEnabled = SettingsManager.isDetectionEnabled(this)

        setContent {
            SmsFraudDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        hasSmsPermission = hasSmsPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        detectionEnabled = detectionEnabled,
                        onToggleDetection = { enabled -> setSmsDetectionEnabled(enabled) },
                        onRequestPermissions = { requestPermissions() },
                        onViewHistory = {
                            val intent = Intent(this@MainActivity, HistoryActivity::class.java)
                            startActivity(intent)
                        },
                        onDownloadModels = {
                            val intent = Intent(this@MainActivity, ModelDownloadActivity::class.java)
                            startActivity(intent)
                        },
                        onDebugLlm = {
                            val intent = Intent(this@MainActivity, LlmDebugActivity::class.java)
                            startActivity(intent)
                        },
                        onRedoOnboarding = {
                            val intent = Intent(this@MainActivity, com.example.smsfrauddetector.onboarding.OnboardingActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
    
    private fun checkPermissions() {
        hasSmsPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    hasSmsPermission: Boolean,
    hasNotificationPermission: Boolean,
    detectionEnabled: Boolean,
    onToggleDetection: (Boolean) -> Unit,
    onRequestPermissions: () -> Unit,
    onViewHistory: () -> Unit = {},
    onDownloadModels: () -> Unit = {},
    onDebugLlm: () -> Unit = {},
    onRedoOnboarding: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title
        Text(
            text = "SMS Fraud Detector",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Detection Toggle Button
        if (hasSmsPermission && hasNotificationPermission) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (detectionEnabled) "Automatic Detection is ON" else "Automatic Detection is OFF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = detectionEnabled,
                    onCheckedChange = { onToggleDetection(it) }
                )
            }
        }

        // App Description
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "How it works:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• Scans all incoming SMS for fraud using rules and on-device AI\n" +
                          "• All detection is private and local—no network needed\n" +
                          "• Shows instant notifications for fraud or safe messages\n" +
                          "• Keeps a history of all analyzed messages\n" +
                          "• You can share SMS or text to the app to check for fraud manually",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Permission Status
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Permission Status:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                PermissionStatusRow("SMS Access", hasSmsPermission)
                PermissionStatusRow("Notifications", hasNotificationPermission)

                Spacer(modifier = Modifier.height(8.dp))

                if (!hasSmsPermission || !hasNotificationPermission) {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant Permissions")
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            text = if (detectionEnabled)
                                "✅ App is ready! SMS fraud detection is now active."
                            else
                                "⏸️ Detection is OFF. SMS messages will not be monitored.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }


        // History Button
        if (hasSmsPermission && hasNotificationPermission) {
            Button(
                onClick = onViewHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("📋 View Message History")
            }
        }

        // LLM Buttons
        if (hasSmsPermission && hasNotificationPermission) {
            Button(
                onClick = onDownloadModels,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⬇️ Download LLM Models")
            }
            Button(
                onClick = onDebugLlm,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🐞 Debug LLM")
            }
        }
            // Redo Onboarding Button
            if (hasSmsPermission && hasNotificationPermission) {
                Button(
                    onClick = onRedoOnboarding,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("🔄 Redo Onboarding")
                }
            }
    }
}

@Composable
fun PermissionStatusRow(name: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name)
        Text(
            text = if (granted) "✅ Granted" else "❌ Not Granted",
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    SmsFraudDetectorTheme {
        MainScreen(
            hasSmsPermission = true,
            hasNotificationPermission = true,
            detectionEnabled = true,
            onToggleDetection = {},
            onRequestPermissions = {},
            onViewHistory = {},
            onDownloadModels = {},
            onDebugLlm = {},
            onRedoOnboarding = {}
        )
    }
}