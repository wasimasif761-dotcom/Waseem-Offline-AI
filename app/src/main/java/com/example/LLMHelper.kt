package com.example

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class ModelMissingException(message: String) : Exception(message)

/**
 * Offline LLM Inference Helper using MediaPipe GenAI LlmInference for Qwen quantized models.
 * Injects local Room database RAG context and formats prompts with Waseem Asif's assistant profile.
 */
class LLMHelper(private val context: Context) {

    companion object {
        const val MODEL_FILENAME = "qwen_model.bin"
        const val MISSING_MODEL_MESSAGE = "Please download the AI model first using the button above."

        const val BASE_SYSTEM_PROMPT = """You are Waseem Asif's personal offline AI assistant.
Developer: Waseem Asif (wasimasif761@gmail.com).

Core Directives:
1. Multilingual Mastery: You are fluent in English, Urdu (اردو), and Arabic (العربية). You MUST reply in the exact language the user used (Urdu in authentic Nastaliq Urdu, Arabic in Modern Standard Arabic, or English).
2. Coding & Technology: Provide clean, bug-free code in HTML, CSS, JavaScript, and Python with clear explanations.
3. Pharmacy & Medicine: Offer accurate insights on pharmaceutical dosage calculations (Clark's, Young's rules), critical drug interactions, and pharmacology.
4. Professional Business: Draft compelling executive emails, business proposals, and strategic advice.
5. Tone: Helpful, direct, highly professional, and courteous."""
    }

    private var llmInference: LlmInference? = null
    private val inferenceMutex = Mutex()
    private var isInitializing = false

    val modelFile: File
        get() {
            val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
            return File(baseDir, MODEL_FILENAME)
        }

    fun isModelReady(): Boolean {
        return modelFile.exists() && modelFile.length() > 10 * 1024 * 1024 // > 10MB
    }

    /**
     * Initializes the MediaPipe LlmInference engine from the downloaded model file.
     */
    suspend fun initializeEngine(): Result<Unit> = withContext(Dispatchers.IO) {
        inferenceMutex.withLock {
            if (llmInference != null) {
                return@withContext Result.success(Unit)
            }

            if (!isModelReady()) {
                return@withContext Result.failure(ModelMissingException(MISSING_MODEL_MESSAGE))
            }

            isInitializing = true
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .setTemperature(0.7f)
                    .setTopK(40)
                    .build()

                llmInference = LlmInference.createFromOptions(context, options)
                isInitializing = false
                Result.success(Unit)
            } catch (e: Exception) {
                isInitializing = false
                llmInference = null
                Result.failure(e)
            }
        }
    }

    /**
     * Constructs the full Qwen ChatML prompt including system directives,
     * injected RAG context from the local Room database, and conversation turns.
     */
    fun buildFullPrompt(
        userMessage: String,
        ragContext: String,
        chatHistory: List<Pair<String, String>> = emptyList() // Pair(role, message)
    ): String {
        return buildString {
            // System block
            append("<|im_start|>system\n")
            append(BASE_SYSTEM_PROMPT.trim())

            if (ragContext.isNotBlank()) {
                append("\n\n")
                append(ragContext.trim())
                append("\nUse the above reference context to answer the user's inquiry accurately whenever applicable.")
            }
            append("<|im_end|>\n")

            // Prior conversation turns (limited to last 4 for memory safety on 4GB RAM devices)
            val recentTurns = chatHistory.takeLast(4)
            for ((role, text) in recentTurns) {
                append("<|im_start|>$role\n")
                append(text.trim())
                append("<|im_end|>\n")
            }

            // Current user turn
            append("<|im_start|>user\n")
            append(userMessage.trim())
            append("<|im_end|>\n")

            // Assistant prompt initiation
            append("<|im_start|>assistant\n")
        }
    }

    /**
     * Generates a response using the on-device LlmInference model.
     * Throws ModelMissingException with MISSING_MODEL_MESSAGE if the model is not downloaded.
     */
    suspend fun generateResponse(
        userMessage: String,
        ragContext: String,
        chatHistory: List<Pair<String, String>> = emptyList()
    ): String = withContext(Dispatchers.Default) {
        if (!isModelReady()) {
            throw ModelMissingException(MISSING_MODEL_MESSAGE)
        }

        inferenceMutex.withLock {
            if (llmInference == null) {
                val initResult = initializeEngine()
                if (initResult.isFailure) {
                    throw initResult.exceptionOrNull()
                        ?: Exception("Failed to initialize offline AI engine.")
                }
            }

            val inference = llmInference ?: throw ModelMissingException(MISSING_MODEL_MESSAGE)
            val prompt = buildFullPrompt(userMessage, ragContext, chatHistory)

            val rawResponse = try {
                inference.generateResponse(prompt)
            } catch (e: Exception) {
                // If model inference fails due to native memory or state, reset instance
                try {
                    inference.close()
                } catch (_: Exception) {}
                llmInference = null
                throw Exception("Inference error: ${e.localizedMessage ?: "Out of memory or unsupported operation"}")
            }

            cleanModelResponse(rawResponse)
        }
    }

    /**
     * Cleans up raw tokens such as <|im_end|>, <|endoftext|>, etc.
     */
    private fun cleanModelResponse(raw: String): String {
        return raw.replace("<|im_end|>", "")
            .replace("<|endoftext|>", "")
            .replace("<|im_start|>", "")
            .trim()
    }

    /**
     * Closes native model inference resources to free device RAM.
     */
    fun close() {
        try {
            llmInference?.close()
        } catch (_: Exception) {}
        llmInference = null
    }
}
