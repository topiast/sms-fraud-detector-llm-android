package com.example.smsfrauddetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Log
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.smsfrauddetector.SettingsManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smsfrauddetector.llm.Accelerator
import com.example.smsfrauddetector.llm.Model
import com.example.smsfrauddetector.llm.ModelManagerViewModel
import com.example.smsfrauddetector.llm.ViewModelFactory
import com.example.smsfrauddetector.llm.createLlmChatConfigs
import com.example.smsfrauddetector.llm.loadAndProcessLlmMessage
import com.example.smsfrauddetector.ui.theme.SmsFraudDetectorTheme
import kotlinx.coroutines.launch
import java.io.File

class LlmDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmsFraudDetectorTheme {
                LlmDebugScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LlmDebugScreen() {
    val DEFAULT_SYSTEM_PROMPT = """Task:
Evaluate whether an SMS message is fraudulent, scam, or spam. Output a score from 0.00 to 1.00, where 1.00 = high confidence of fraud.
Provide a brief, factual explanation based on tone, structure, and content. Focus on intentions (e.g., wants user to click a link, provide personal info). Many messages might seem suspicious but lack intentions to cause harm, and are therefore likely safe.
If message is suspicious, you should tell it but also state that if the message is expected or known by the user, it may be safe.
Use the following format:
Reason: <explanation>
Score: <score>
---
Fraud Indicators:
- Impersonates trusted entities (e.g., banks, gov.)
- Creates urgency, fear, or legal pressure
- Requests personal/financial info
- Includes suspicious, misspelled, or non-official links

- Legitimate Traits:
- Clear, professional tone
- No pressure or threats
- Sender/links are verifiable
- Contextually expected (e.g., user-initiated action)
---
Examples:
Message:
Your Airbnb verification code is: 714564. Don't share this code with anyone; our employees will never ask for the code.
Reason: Professional tone, no urgency, standard 2FA pattern, no links.
Score: 0.05

Message:
Olet saanut maksamattoman tullimaksun. Vältä lisämaksut maksamalla nyt: fi-tullivero.com
Reason: Creates urgency, impersonates customs authority, includes unofficial-looking link.
Score: 0.93
---
Now analyze the following message:
"""

    val context = LocalContext.current as ComponentActivity
    val modelManagerViewModel: ModelManagerViewModel = viewModel(factory = ViewModelFactory(context))
    val scope = rememberCoroutineScope()
    var systemPrompt by remember { mutableStateOf(DEFAULT_SYSTEM_PROMPT) }
    var prompt by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }



    val models = com.example.smsfrauddetector.llm.ModelRegistry.models
    LaunchedEffect(Unit) {
        modelManagerViewModel.setModels(models)
    }
    val uiState by modelManagerViewModel.uiState.collectAsState()
    val downloadedModels = uiState.models.filter {
        uiState.modelDownloadStatus[it.name]?.status == com.example.smsfrauddetector.llm.ModelDownloadStatusType.SUCCEEDED
    }
    // Initialize selectedModel from persisted settings
    var selectedModel by remember(downloadedModels) {
        val selectedName = SettingsManager.getSelectedModelName(context)
        mutableStateOf(downloadedModels.find { it.name == selectedName } ?: downloadedModels.firstOrNull())
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LLM Debug") },
                navigationIcon = {
                    IconButton(onClick = { context.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedModel?.name ?: "Select a model",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        downloadedModels.forEach { model ->
                            DropdownMenuItem(
                                text = { Text(model.name) },
                                onClick = {
                                    selectedModel = model
                                    expanded = false
                                    Log.d("LlmDebugActivity", "Model selected in debug view: ${model.name} (not persisted)")
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = { systemPrompt = it },
                    label = { Text("Enter system prompt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp, max = 180.dp),
                    maxLines = Int.MAX_VALUE
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Enter prompt") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 320.dp),
                    maxLines = Int.MAX_VALUE
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        selectedModel?.let { model ->
                            model.preProcess()
                            scope.launch {
                                Log.d("LlmDebugActivity", "Sending message to LLM...")
                                status = "Loading model..."
                                result = ""
                                Log.d("LlmDebugActivity", "Status set to Loading model")
                                status = "Generating response..."
                                Log.d("LlmDebugActivity", "Status set to Generating response")
                                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    loadAndProcessLlmMessage(context, model, prompt, systemPrompt)
                                }
                                Log.d("LlmDebugActivity", "LLM response received: $response")
                                result = response
                                status = "Done"
                            }
                        }
                    },
                    enabled = selectedModel != null
                ) {
                    Text("Send to LLM")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Status: $status")
                if (status == "Loading model..." || status == "Generating response...") {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator()
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Result: $result")
            }
        }
    }
}