package com.example.smsfrauddetector

import android.content.Context
import com.example.smsfrauddetector.llm.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FraudDetector {

    companion object {
        private val SUSPICIOUS_KEYWORDS = listOf(
            "fraud", "scam", "urgent", "verify", "suspend", "click", "winner",
            "congratulations", "prize", "free", "claim", "expire", "account suspended"
        )

        private const val SYSTEM_PROMPT = """Task:
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
    }

    /**
     * Analyzes an SMS message for fraud using a pre-initialized LLM instance.
     * This is the primary method for fraud detection in the app.
     *
     * @param context The application context.
     * @param model The metadata for the LLM model being used.
     * @param llmInference The initialized LlmInference instance.
     * @param messageBody The content of the SMS message.
     * @param sender The sender of the SMS message.
     * @return A [FraudAnalysisResult] containing the analysis details.
     */
    suspend fun analyzeMessage(
        context: Context,
        model: Model,
        llmInference: com.google.mediapipe.tasks.genai.llminference.LlmInference,
        messageBody: String,
        sender: String?
    ): FraudAnalysisResult {
        val prompt = "Message:\n$messageBody"
        val llmResponse = withContext(Dispatchers.IO) {
            llmInference.generateResponse(SYSTEM_PROMPT + "\n" + prompt)
        }

        // Parse LLM response
        var score = 0.0
        var reason = ""
        val scoreRegex = Regex("""Score:\s*([0-9.]+)""")
        val reasonRegex = Regex("""Reason:\s*(.+)""")
        scoreRegex.find(llmResponse)?.groupValues?.getOrNull(1)?.let {
            score = it.toDoubleOrNull() ?: 0.0
        }
        reasonRegex.find(llmResponse)?.groupValues?.getOrNull(1)?.let {
            reason = it.trim()
        }

        // Find suspicious words
        val normalizedMessage = messageBody.lowercase().trim()
        val foundSuspiciousWords = SUSPICIOUS_KEYWORDS.filter { normalizedMessage.contains(it) }

        val isFraud = score >= 0.5

        return FraudAnalysisResult(
            isFraud = isFraud,
            confidence = score,
            suspiciousWords = foundSuspiciousWords,
            reasons = listOf(reason),
            sender = sender,
            messageBody = messageBody
        )
    }
    /**
     * Legacy suspicious keyword method for compatibility/testing and as a fallback.
     */
    suspend fun analyzeMessageLegacy(messageBody: String, sender: String?): FraudAnalysisResult {
        // Artificial delay for debugging legacy detector
        // This delay is 10 seconds (10000 milliseconds)
        kotlinx.coroutines.delay(10_000)

        val normalizedMessage = messageBody.lowercase().trim()
        val foundSuspiciousWords = SUSPICIOUS_KEYWORDS.filter { normalizedMessage.contains(it) }
        val isFraud = foundSuspiciousWords.isNotEmpty()
        val confidence = if (isFraud) 0.95 else 0.05
        val reasons = if (isFraud) {
            listOf("Contains suspicious keywords: ${foundSuspiciousWords.joinToString(", ")}")
        } else {
            listOf("No suspicious keywords detected")
        }
        return FraudAnalysisResult(
            isFraud = isFraud,
            confidence = confidence,
            suspiciousWords = foundSuspiciousWords,
            reasons = reasons,
            sender = sender,
            messageBody = messageBody
        )
    }
}