package com.example

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Knowledge item entity stored in the local Room database for offline RAG injection.
 */
@Entity(tableName = "knowledge_base")
data class KnowledgeItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val category: String, // "Profile", "Pharmacy", "Coding", "Translation", "Business", "Custom"
    val source: String,   // "Built-in", "Pasted Text", "Web URL"
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_base ORDER BY timestamp DESC")
    fun getAllKnowledge(): Flow<List<KnowledgeItem>>

    @Query("SELECT * FROM knowledge_base WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchKnowledge(query: String): Flow<List<KnowledgeItem>>

    @Query("SELECT * FROM knowledge_base ORDER BY timestamp DESC")
    suspend fun getAllKnowledgeList(): List<KnowledgeItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: KnowledgeItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<KnowledgeItem>)

    @Update
    suspend fun update(item: KnowledgeItem)

    @Delete
    suspend fun delete(item: KnowledgeItem)

    @Query("DELETE FROM knowledge_base WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM knowledge_base")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM knowledge_base")
    suspend fun count(): Int
}

@Database(entities = [KnowledgeItem::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun knowledgeDao(): KnowledgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "waseem_offline_ai.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Database helper handling Room persistence, initial seeding, URL fetching,
 * and contextual retrieval for RAG prompt augmentation.
 */
class DatabaseHelper(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.knowledgeDao()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getAllKnowledge(): Flow<List<KnowledgeItem>> = dao.getAllKnowledge()

    fun searchKnowledge(query: String): Flow<List<KnowledgeItem>> = dao.searchKnowledge(query)

    suspend fun insertItem(
        title: String,
        content: String,
        category: String = "Custom",
        source: String = "Pasted Text"
    ): Long {
        return dao.insert(
            KnowledgeItem(
                title = title.trim(),
                content = content.trim(),
                category = category,
                source = source
            )
        )
    }

    suspend fun deleteItem(item: KnowledgeItem) = dao.delete(item)

    suspend fun deleteItemById(id: Long) = dao.deleteById(id)

    suspend fun clearAllKnowledge() = dao.clearAll()

    /**
     * Seeds initial curated reference knowledge if the database is empty.
     */
    suspend fun seedInitialKnowledgeIfNeeded() = withContext(Dispatchers.IO) {
        if (dao.count() == 0) {
            val defaults = listOf(
                KnowledgeItem(
                    title = "Waseem Asif Profile & Assistant Guidelines",
                    category = "Profile",
                    source = "Built-in",
                    content = """
                        Developer: Waseem Asif
                        Email: wasimasif761@gmail.com
                        Role of AI: Waseem Asif's Personal Offline AI Assistant.
                        Directives:
                        1. Always provide direct, polite, and technically sound answers.
                        2. Excel in multilingual support: fluent English, authentic Urdu (اردو), and formal Modern Standard Arabic (العربية).
                        3. Specialize in software engineering, pharmacy inquiries, professional business letters, and translation tasks.
                    """.trimIndent()
                ),
                KnowledgeItem(
                    title = "Clinical Pharmacy Dosage Formulas & Interactions",
                    category = "Pharmacy",
                    source = "Built-in",
                    content = """
                        Clinical Pharmacy Reference:
                        - Clark's Rule (Pediatric Dosage): (Weight in lbs / 150) * Adult Dose.
                        - Young's Rule (Pediatric Dosage): (Age in years / (Age + 12)) * Adult Dose.
                        - Critical Drug-Drug Interactions:
                          * Warfarin + NSAIDs (Aspirin/Ibuprofen): Significant bleeding and GI ulceration risk.
                          * ACE Inhibitors (Lisinopril) + Spironolactone: Severe Hyperkalemia risk.
                          * Simvastatin + Clarithromycin: Acute Rhabdomyolysis risk via CYP3A4 inhibition.
                        - Dispensing checklist: Confirm patient allergies, eGFR/renal status, pregnancy tier, and food co-administration.
                    """.trimIndent()
                ),
                KnowledgeItem(
                    title = "Multilingual Translation Guide (Urdu & Arabic)",
                    category = "Translation",
                    source = "Built-in",
                    content = """
                        Translation Principles:
                        - Urdu (اردو): Use polite respectful terminology (آپ جناب, تشریف آوری, معزز). Match standard Nastaliq grammar for business, legal, and everyday communications.
                        - Arabic (العربية): Adhere to Modern Standard Arabic (فصحى). Use classic formal salutations (السلام عليكم ورحمة الله وبركاته, مع فائق الاحترام والتقدير) for business documents.
                        - Technical terms in translations: Keep code identifiers, variable names, and pharmaceutical compound names clear with concise localized explanations.
                    """.trimIndent()
                ),
                KnowledgeItem(
                    title = "Full-Stack Web & Python Development Patterns",
                    category = "Coding",
                    source = "Built-in",
                    content = """
                        Programming Best Practices:
                        - Python: Use type hints (typing), dataclasses, list/dict comprehensions, async/await with asyncio, and PEP-8 conventions.
                        - JavaScript / HTML / CSS: Semantic HTML5, CSS Flexbox/Grid responsive architecture, clean ES6+ modular code, and modern accessible UI design.
                        - Android: Jetpack Compose, unidirectional data flow (UDF), ViewModel with StateFlow, Room local database, coroutines.
                    """.trimIndent()
                ),
                KnowledgeItem(
                    title = "Executive Business Email & Proposal Templates",
                    category = "Business",
                    source = "Built-in",
                    content = """
                        Business Correspondence:
                        - Executive Email Structure:
                          1. Clear, compelling Subject Line.
                          2. Professional Salutation.
                          3. Core objective in first 2 sentences.
                          4. Bulleted key details, timeline, and deliverables.
                          5. Unambiguous Call-to-Action (CTA) with deadline.
                          6. Professional Sign-off with contact details.
                        - Business negotiation: Focus on win-win value propositions, clear risk mitigation, and mutual ROI.
                    """.trimIndent()
                )
            )
            dao.insertAll(defaults)
        }
    }

    /**
     * Fetches plain text or article body from a given web URL.
     * Extracts text cleanly from HTML or raw text feeds.
     */
    suspend fun fetchTextFromUrl(urlString: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                "https://$urlString"
            } else {
                urlString
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; WaseemOfflineAI/1.0)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val rawBody = response.body?.string().orEmpty()
            if (rawBody.isBlank()) {
                return@withContext Result.failure(Exception("URL returned empty content"))
            }

            // Clean HTML tags and extract readable text
            val cleanedText = cleanHtml(rawBody)
            Result.success(cleanedText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanHtml(html: String): String {
        var text = html
        // Remove scripts and styles
        text = text.replace(Regex("(?s)<script.*?</script>"), " ")
        text = text.replace(Regex("(?s)<style.*?</style>"), " ")
        // Replace paragraph and break tags with newlines
        text = text.replace(Regex("(?i)<(p|div|br|h[1-6]|li) */?>"), "\n")
        // Strip remaining HTML tags
        text = text.replace(Regex("<[^>]+>"), " ")
        // Decode common HTML entities
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        // Collapse multiple blank lines
        text = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        // Limit to reasonable context size (first 10,000 characters)
        return if (text.length > 10000) text.substring(0, 10000) + "\n...[Content truncated]" else text
    }

    /**
     * Retrieves relevant context snippets based on user query keywords,
     * or returns the top curated knowledge items to inject into the system prompt.
     */
    suspend fun getRAGContext(userQuery: String, maxItems: Int = 4): String = withContext(Dispatchers.IO) {
        val allItems = dao.getAllKnowledgeList()
        if (allItems.isEmpty()) return@withContext ""

        val queryWords = userQuery.lowercase()
            .split(Regex("[\\s,?.!;:]+"))
            .filter { it.length > 2 }

        // Rank items by keyword match relevance
        val scoredItems = allItems.map { item ->
            var score = 0
            val itemText = (item.title + " " + item.content).lowercase()
            for (word in queryWords) {
                if (itemText.contains(word)) {
                    score += 2
                }
            }
            // Always give a baseline score to Profile items
            if (item.category == "Profile") {
                score += 1
            }
            item to score
        }.sortedByDescending { it.second }

        val selected = scoredItems.take(maxItems).map { it.first }

        buildString {
            append("=== LOCAL KNOWLEDGE BASE CONTEXT (OFFLINE RAG) ===\n")
            selected.forEachIndexed { index, item ->
                append("\n[Doc ${index + 1}: ${item.title} (Category: ${item.category})]\n")
                // Truncate individual items if overly long to prevent token overflow
                val snippet = if (item.content.length > 1500) {
                    item.content.substring(0, 1500) + "..."
                } else {
                    item.content
                }
                append(snippet.trim())
                append("\n")
            }
            append("\n=== END OF CONTEXT ===\n")
        }
    }
}
