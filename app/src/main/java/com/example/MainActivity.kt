package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.AiBubbleDark
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.CodeBlockDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.ErrorDark
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceHighDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.UserBubbleDark
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chat message model for ChatGPT-like interface.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user", "assistant", "system"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false
)

/**
 * Main ViewModel managing chat interactions, offline Qwen inference,
 * Room RAG knowledge base sync, and model downloader state.
 */
class MainViewModel(
    private val databaseHelper: DatabaseHelper,
    private val modelDownloader: ModelDownloader,
    private val llmHelper: LLMHelper
) : ViewModel() {

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isModelReady = MutableStateFlow(llmHelper.isModelReady())
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    val downloadState: StateFlow<DownloadState> = modelDownloader.downloadState
    val knowledgeList = databaseHelper.getAllKnowledge()

    private var activeInferenceJob: Job? = null

    init {
        viewModelScope.launch {
            databaseHelper.seedInitialKnowledgeIfNeeded()
            checkModelStatus()
        }

        // Add initial welcoming message
        _messages.add(
            ChatMessage(
                sender = "assistant",
                text = "Welcome to Waseem Offline AI! I am your 100% offline Personal AI Assistant, created by Waseem Asif (wasimasif761@gmail.com).\n\n" +
                        "I am fully optimized for on-device execution (4GB RAM devices) with support for:\n" +
                        "• English, Urdu (اردو), and Arabic (العربية) translation\n" +
                        "• Clean code in Python, JavaScript, and HTML/CSS\n" +
                        "• Clinical pharmacy calculations, dosages & drug interactions\n" +
                        "• Professional business proposals & executive emails\n" +
                        "• Offline RAG Knowledge Base\n\n" +
                        if (!llmHelper.isModelReady()) {
                            "Notice: Please tap 'Download Qwen AI Model' above to enable offline AI generation."
                        } else {
                            "Offline neural model is loaded and ready. How may I assist you?"
                        }
            )
        )
    }

    fun checkModelStatus() {
        _isModelReady.value = llmHelper.isModelReady()
    }

    fun startModelDownload(url: String = ModelDownloader.URL_QWEN_1_5B) {
        modelDownloader.startDownload(viewModelScope, url) {
            checkModelStatus()
            viewModelScope.launch {
                llmHelper.initializeEngine()
            }
            _messages.add(
                ChatMessage(
                    sender = "system",
                    text = "Qwen AI Model downloaded and loaded successfully! The assistant is now 100% offline."
                )
            )
        }
    }

    fun cancelModelDownload() {
        modelDownloader.cancelDownload()
    }

    fun deleteModel() {
        modelDownloader.deleteModel()
        llmHelper.close()
        checkModelStatus()
        _messages.add(
            ChatMessage(
                sender = "system",
                text = "AI model file deleted from local storage."
            )
        )
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        // Add user message
        _messages.add(ChatMessage(sender = "user", text = trimmed))

        // Check model readiness
        if (!llmHelper.isModelReady()) {
            _messages.add(
                ChatMessage(
                    sender = "assistant",
                    text = LLMHelper.MISSING_MODEL_MESSAGE,
                    isError = true
                )
            )
            return
        }

        // Run real AI inference
        _isGenerating.value = true
        activeInferenceJob = viewModelScope.launch {
            try {
                // Fetch relevant RAG context from local Room DB
                val ragContext = databaseHelper.getRAGContext(trimmed)

                // Build history turns
                val history = _messages.filter { it.sender != "system" }
                    .takeLast(6)
                    .map { (if (it.sender == "user") "user" else "assistant") to it.text }

                // Offline generation
                val response = llmHelper.generateResponse(trimmed, ragContext, history)

                _messages.add(
                    ChatMessage(
                        sender = "assistant",
                        text = response
                    )
                )
            } catch (e: ModelMissingException) {
                _messages.add(
                    ChatMessage(
                        sender = "assistant",
                        text = LLMHelper.MISSING_MODEL_MESSAGE,
                        isError = true
                    )
                )
            } catch (e: Exception) {
                _messages.add(
                    ChatMessage(
                        sender = "assistant",
                        text = "AI Error: ${e.localizedMessage ?: "Unknown error occurred during inference"}",
                        isError = true
                    )
                )
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun stopGeneration() {
        activeInferenceJob?.cancel()
        _isGenerating.value = false
    }

    fun clearChat() {
        _messages.clear()
        _messages.add(
            ChatMessage(
                sender = "assistant",
                text = "Chat history cleared. Offline AI assistant is ready."
            )
        )
    }

    // Knowledge management
    fun addKnowledge(title: String, content: String, category: String, source: String, onDone: () -> Unit) {
        viewModelScope.launch {
            databaseHelper.insertItem(title, content, category, source)
            onDone()
        }
    }

    fun fetchAndAddFromUrl(url: String, title: String, category: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = databaseHelper.fetchTextFromUrl(url)
            if (result.isSuccess) {
                val text = result.getOrNull().orEmpty()
                val finalTitle = title.ifBlank { "Web Source: $url" }
                databaseHelper.insertItem(finalTitle, text, category, "URL: $url")
                onResult(true, "Knowledge imported successfully!")
            } else {
                onResult(false, result.exceptionOrNull()?.localizedMessage ?: "Failed to fetch URL")
            }
        }
    }

    fun deleteKnowledge(id: Long) {
        viewModelScope.launch {
            databaseHelper.deleteItemById(id)
        }
    }

    fun getModelFilePath(): String = modelDownloader.targetFile.absolutePath
    fun getModelFileSize(): String = modelDownloader.getModelFileSizeFormatted()

    override fun onCleared() {
        super.onCleared()
        llmHelper.close()
    }
}

class MainViewModelFactory(
    private val databaseHelper: DatabaseHelper,
    private val modelDownloader: ModelDownloader,
    private val llmHelper: LLMHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(databaseHelper, modelDownloader, llmHelper) as T
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val databaseHelper = DatabaseHelper(this)
        val modelDownloader = ModelDownloader(this)
        val llmHelper = LLMHelper(this)
        val factory = MainViewModelFactory(databaseHelper, modelDownloader, llmHelper)

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel(factory = factory)
                WaseemAiApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaseemAiApp(viewModel: MainViewModel) {
    val messages = viewModel.messages
    val isGenerating by viewModel.isGenerating.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val knowledgeList by viewModel.knowledgeList.collectAsState(initial = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var userInput by remember { mutableStateOf("") }
    var showKnowledgeSheet by remember { mutableStateOf(false) }
    var showModelSettingsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Waseem Offline AI",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // Status indicator pill
                            ModelStatusPill(isModelReady = isModelReady, downloadState = downloadState)
                        }
                        Text(
                            text = "by Waseem Asif • 100% On-Device Qwen LLM",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark,
                    titleContentColor = TextPrimaryDark
                ),
                actions = {
                    // Knowledge Base (RAG) button
                    IconButton(
                        onClick = { showKnowledgeSheet = true },
                        modifier = Modifier.testTag("knowledge_button")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Knowledge Base (RAG)",
                                tint = CyanPrimary
                            )
                            if (knowledgeList.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .background(VioletSecondary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${knowledgeList.size.coerceAtMost(99)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Model Manager button
                    IconButton(
                        onClick = { showModelSettingsDialog = true },
                        modifier = Modifier.testTag("model_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Model Manager",
                            tint = TextSecondaryDark
                        )
                    }

                    // Clear Chat button
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear Chat",
                            tint = TextSecondaryDark
                        )
                    }

                    // About App / Developer button
                    IconButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("about_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About Waseem Asif",
                            tint = TextSecondaryDark
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            ChatBottomInputBar(
                text = userInput,
                onTextChange = { userInput = it },
                isGenerating = isGenerating,
                onSend = {
                    if (userInput.isNotBlank()) {
                        val text = userInput
                        userInput = ""
                        viewModel.sendMessage(text)
                    }
                },
                onStop = { viewModel.stopGeneration() }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundDark)
        ) {
            // Prominent Model Downloader Banner (visible when model is missing or currently downloading)
            AnimatedVisibility(
                visible = !isModelReady || downloadState is DownloadState.Downloading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                ModelDownloaderBanner(
                    downloadState = downloadState,
                    onDownload15B = { viewModel.startModelDownload(ModelDownloader.URL_QWEN_1_5B) },
                    onDownload05B = { viewModel.startModelDownload(ModelDownloader.URL_QWEN_0_5B) },
                    onCancel = { viewModel.cancelModelDownload() }
                )
            }

            // Quick Prompt Suggestions Row
            QuickPromptSuggestions(
                onSelectPrompt = { prompt ->
                    userInput = prompt
                }
            )

            // Message List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageBubble(
                        message = msg,
                        onCopy = { textToCopy ->
                            clipboardManager.setText(AnnotatedString(textToCopy))
                            scope.launch {
                                snackbarHostState.showSnackbar("Copied to clipboard!")
                            }
                        }
                    )
                }

                if (isGenerating) {
                    item {
                        GeneratingIndicator()
                    }
                }
            }
        }
    }

    // Knowledge Base (RAG) Bottom Sheet
    if (showKnowledgeSheet) {
        KnowledgeBaseBottomSheet(
            knowledgeList = knowledgeList,
            onDismiss = { showKnowledgeSheet = false },
            onAddKnowledge = { title, content, cat, src ->
                viewModel.addKnowledge(title, content, cat, src) {
                    scope.launch { snackbarHostState.showSnackbar("Knowledge saved to local database!") }
                }
            },
            onFetchUrl = { url, title, cat ->
                viewModel.fetchAndAddFromUrl(url, title, cat) { success, msg ->
                    scope.launch { snackbarHostState.showSnackbar(msg) }
                }
            },
            onDelete = { id ->
                viewModel.deleteKnowledge(id)
            }
        )
    }

    // Model Manager Dialog
    if (showModelSettingsDialog) {
        ModelManagerDialog(
            isModelReady = isModelReady,
            modelPath = viewModel.getModelFilePath(),
            modelSize = viewModel.getModelFileSize(),
            downloadState = downloadState,
            onDownload = { viewModel.startModelDownload() },
            onDelete = { viewModel.deleteModel() },
            onDismiss = { showModelSettingsDialog = false }
        )
    }

    // About Waseem Asif Dialog
    if (showAboutDialog) {
        AboutAppDialog(onDismiss = { showAboutDialog = false })
    }
}

/**
 * Status Pill indicating model state in TopAppBar.
 */
@Composable
fun ModelStatusPill(isModelReady: Boolean, downloadState: DownloadState) {
    val (bgColor, dotColor, text) = when {
        downloadState is DownloadState.Downloading -> Triple(
            CyanPrimary.copy(alpha = 0.2f),
            CyanPrimary,
            "Downloading"
        )
        isModelReady -> Triple(
            EmeraldTertiary.copy(alpha = 0.2f),
            EmeraldTertiary,
            "Offline Ready"
        )
        else -> Triple(
            ErrorDark.copy(alpha = 0.2f),
            ErrorDark,
            "Model Required"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Prominent In-App Model Downloader Banner.
 * Must be prominently visible until the model is downloaded.
 */
@Composable
fun ModelDownloaderBanner(
    downloadState: DownloadState,
    onDownload15B: () -> Unit,
    onDownload05B: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("model_downloader_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorderDark))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(CyanPrimary.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = CyanPrimary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Download Qwen AI Model",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "100% offline on-device neural model (Qwen quantized)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (downloadState) {
                is DownloadState.Downloading -> {
                    Column {
                        LinearProgressIndicator(
                            progress = { downloadState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyanPrimary,
                            trackColor = SurfaceHighDark,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${(downloadState.progress * 100).toInt()}% • ${downloadState.speedFormatted}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = downloadState.statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedDark
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("cancel_download_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorDark)
                        ) {
                            Text("Cancel Download")
                        }
                    }
                }

                is DownloadState.Error -> {
                    Text(
                        text = "Download Error: ${downloadState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onDownload15B,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Retry Download", color = Color.Black)
                    }
                }

                else -> {
                    Text(
                        text = "Save directly to device storage for unlimited, private offline chat. Excellent for coding, Urdu/Arabic translation, pharmacy & emails.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDownload15B,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("download_model_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Download Qwen (~1.2 GB)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = onDownload05B,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VioletSecondary),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(VioletSecondary.copy(alpha = 0.5f)))
                        ) {
                            Text("Lite (~350MB)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Prompt Suggestions row to help test features instantly.
 */
@Composable
fun QuickPromptSuggestions(onSelectPrompt: (String) -> Unit) {
    val prompts = listOf(
        "Translate to Urdu: How are you today?" to "اردو ترجمہ",
        "Translate to Arabic: Welcome to our company." to "عربي ترجمة",
        "Write Python script to parse JSON and calculate average." to "Python Code",
        "Draft a formal business partnership email." to "Business Email",
        "Pharmacy: What is Clark's rule for pediatric dosage?" to "Pharmacy Dose",
        "HTML/CSS responsive pricing table with flexbox." to "HTML/CSS Card"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { (fullPrompt, chipLabel) ->
            AssistChip(
                onClick = { onSelectPrompt(fullPrompt) },
                label = { Text(chipLabel, fontSize = 12.sp, color = TextPrimaryDark) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = SurfaceHighDark
                ),
                border = BorderStroke(1.dp, CardBorderDark)
            )
        }
    }
}

/**
 * Individual Chat Message Bubble with AI avatar, code formatting, and Copy button.
 */
@Composable
fun ChatMessageBubble(message: ChatMessage, onCopy: (String) -> Unit) {
    val isUser = message.sender == "user"
    val isSystem = message.sender == "system"

    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = when {
        isUser -> UserBubbleDark
        isSystem -> SurfaceHighDark
        message.isError -> ErrorDark.copy(alpha = 0.15f)
        else -> AiBubbleDark
    }

    val bubbleBorder = when {
        message.isError -> ErrorDark.copy(alpha = 0.5f)
        isUser -> Color.Transparent
        else -> CardBorderDark
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.95f)
        ) {
            if (!isUser && !isSystem) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(CyanPrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = "AI Assistant",
                        tint = CyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .background(
                        color = bubbleColor,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = bubbleBorder,
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    if (message.isError) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ErrorDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Action Required",
                                style = MaterialTheme.typography.labelSmall,
                                color = ErrorDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Render code blocks cleanly if present
                    FormattedMessageText(text = message.text)

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimestamp(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMutedDark,
                            fontSize = 10.sp
                        )

                        if (!isUser && !isSystem) {
                            IconButton(
                                onClick = { onCopy(message.text) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("copy_response_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Response",
                                    tint = TextSecondaryDark,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Text renderer that styles code blocks (```code```) distinctly with monospace font.
 */
@Composable
fun FormattedMessageText(text: String) {
    val parts = text.split("```")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                val lines = part.trim().lines()
                val lang = if (lines.firstOrNull()?.all { it.isLetterOrDigit() } == true) lines.first() else ""
                val codeContent = if (lang.isNotEmpty()) lines.drop(1).joinToString("\n") else part.trim()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CodeBlockDark, RoundedCornerShape(8.dp))
                        .border(1.dp, CardBorderDark, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        if (lang.isNotEmpty()) {
                            Text(
                                text = lang.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        Text(
                            text = codeContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                // Normal text (supports Arabic, Urdu, English seamlessly)
                if (part.isNotBlank()) {
                    Text(
                        text = part.trim(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GeneratingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(SurfaceHighDark, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = CyanPrimary,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Qwen AI is generating offline response...",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondaryDark
        )
    }
}

/**
 * Bottom Input Bar with Send and Stop buttons.
 */
@Composable
fun ChatBottomInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorderDark))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = {
                    Text(
                        "Ask in English, Urdu (اردو), or Arabic (العربية)...",
                        color = TextMutedDark,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp, max = 130.dp)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceVariantDark,
                    unfocusedContainerColor = SurfaceVariantDark,
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = CardBorderDark,
                    focusedTextColor = TextPrimaryDark,
                    unfocusedTextColor = TextPrimaryDark,
                    cursorColor = CyanPrimary
                ),
                maxLines = 5
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (isGenerating) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(48.dp)
                        .background(ErrorDark, CircleShape)
                        .testTag("stop_generation_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Generation",
                        tint = Color.White
                    )
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (text.isNotBlank()) CyanPrimary else SurfaceHighDark,
                            CircleShape
                        )
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message",
                        tint = if (text.isNotBlank()) Color.Black else TextMutedDark
                    )
                }
            }
        }
    }
}

/**
 * Knowledge Base (RAG) Bottom Sheet for adding text, fetching URLs,
 * and reviewing stored documents for offline prompt augmentation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeBaseBottomSheet(
    knowledgeList: List<KnowledgeItem>,
    onDismiss: () -> Unit,
    onAddKnowledge: (String, String, String, String) -> Unit,
    onFetchUrl: (String, String, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Form inputs
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Custom") }
    var urlInput by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("Custom", "Coding", "Pharmacy", "Translation", "Business")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        contentColor = TextPrimaryDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local Knowledge Base (RAG)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "${knowledgeList.size} items stored offline for AI context",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondaryDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceVariantDark,
                contentColor = CyanPrimary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Add Text", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Fetch URL", fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Stored (${knowledgeList.size})", fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Paste Custom Knowledge Text
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title / Topic") },
                        placeholder = { Text("e.g. My Pharmacy Rules, Python Utility") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Category:", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content to Memorize") },
                        placeholder = { Text("Paste code templates, business rules, drug notes, translations...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank() && content.isNotBlank()) {
                                onAddKnowledge(title, content, selectedCategory, "Pasted Text")
                                title = ""
                                content = ""
                            }
                        },
                        enabled = title.isNotBlank() && content.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_knowledge_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Local Knowledge", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                1 -> {
                    // Fetch Knowledge from Web URL
                    Text(
                        text = "Fetch documentation or articles from online URL to save into offline RAG storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondaryDark
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Target URL") },
                        placeholder = { Text("https://example.com/notes.html") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Optional Title") },
                        placeholder = { Text("e.g. Official Guidelines") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (urlInput.isNotBlank()) {
                                isFetching = true
                                onFetchUrl(urlInput, title, selectedCategory)
                                isFetching = false
                                urlInput = ""
                                title = ""
                            }
                        },
                        enabled = urlInput.isNotBlank() && !isFetching,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fetch_url_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetching...", color = Color.Black)
                        } else {
                            Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fetch & Store for Offline", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                2 -> {
                    // Stored Knowledge List
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search stored knowledge...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimaryDark,
                            unfocusedTextColor = TextPrimaryDark
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val filtered = knowledgeList.filter {
                        if (searchQuery.isBlank()) true
                        else it.title.contains(searchQuery, ignoreCase = true) ||
                                it.content.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${item.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = VioletSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.content,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondaryDark,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(onClick = { onDelete(item.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Knowledge",
                                            tint = ErrorDark.copy(alpha = 0.8f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Model Manager Dialog to review model file, path, and storage actions.
 */
@Composable
fun ModelManagerDialog(
    isModelReady: Boolean,
    modelPath: String,
    modelSize: String,
    downloadState: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorderDark))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Qwen AI Model Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    Text(
                        text = if (isModelReady) "Ready (Installed)" else "Not Downloaded",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isModelReady) EmeraldTertiary else ErrorDark
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row {
                    Text("File Size: ", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                    Text(
                        text = modelSize,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Storage Path:\n$modelPath",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (!isModelReady) {
                    Button(
                        onClick = {
                            onDownload()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                    ) {
                        Text("Download Model Now", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorDark)
                    ) {
                        Text("Delete Model to Free Storage")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", color = TextSecondaryDark)
                }
            }
        }
    }
}

/**
 * About Dialog showcasing Waseem Asif credentials and offline AI architecture.
 */
@Composable
fun AboutAppDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorderDark))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Waseem Offline AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = CyanPrimary
                )

                Text(
                    text = "Personal On-Device AI Assistant",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Developer:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark
                )
                Text(
                    text = "Waseem Asif",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryDark
                )
                Text(
                    text = "wasimasif761@gmail.com",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyanPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "System Specs:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedDark
                )
                Text(
                    text = "• AI Engine: MediaPipe LlmInference / LiteRT\n" +
                            "• Quantized Neural Model: Qwen 2.5 (1.5B / 0.5B)\n" +
                            "• Target Hardware: Android 10+ (Optimized for 4GB RAM)\n" +
                            "• RAG: Local Room Database with Web URL Scraper\n" +
                            "• Multilingual: English, Urdu (اردو), Arabic (العربية)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceHighDark)
                ) {
                    Text("OK", color = TextPrimaryDark)
                }
            }
        }
    }
}

fun formatTimestamp(millis: Long): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date(millis))
}
