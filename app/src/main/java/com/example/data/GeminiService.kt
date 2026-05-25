package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class Part(
    @Json(name = "text") val text: String? = null
)

data class Content(
    @Json(name = "parts") val parts: List<Part>
)

data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

data class Candidate(
    @Json(name = "content") val content: Content? = null
)

data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }
}

object GeminiRepository {
    suspend fun fetchGeminiResponse(prompt: String, chatHistory: List<ChatMessage> = emptyList()): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Holographic Chat Offline: API Key not configured. To enable artificial intelligence features, please enter your GEMINI_API_KEY in the Secrets panel."
        }
        
        val contentsList = mutableListOf<Content>()
        
        // Add last 6 messages to preserve token context cleanly
        chatHistory.takeLast(6).forEach { message ->
            contentsList.add(
                Content(parts = listOf(Part(text = message.text)))
            )
        }
        
        // Add current dynamic prompt
        contentsList.add(
            Content(parts = listOf(Part(text = prompt)))
        )

        val sysInstruction = Content(
            parts = listOf(
                Part(text = "You are the GP PANEL AI Assistant—a legendary, stylish, and futuristic gaming companion. Your tone is witty, cyberpunk, elite, neon-green themed, and highly knowledgeable. You excel in explaining weapon tactics, recoil calibration parameters, FPS acceleration settings, DPI boosters, and gaming theory. Remember that all booster features inside this GP Panel app are beautifully executed visual simulations for security and safety. Never suggest actual system hacking, malicious cheating, or anything that violates local policies or real-world security. Keep sentences ultra-cool, structured with terminal bullet points, and under 130 words.")
            )
        )

        val request = GenerateContentRequest(
            contents = contentsList,
            systemInstruction = sysInstruction
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "GP Panel error: Holographic core returned no signal. Please retry terminal transmission."
        } catch (e: Exception) {
            "GP Panel connection error: ${e.localizedMessage ?: "Timeout in holographic frequencies"}. Ensure network capability is active."
        }
    }
}
