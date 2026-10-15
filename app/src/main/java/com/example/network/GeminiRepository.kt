package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiRepository {
    private val modelName = "gemini-3.1-flash-lite-preview"
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val chatListType = Types.newParameterizedType(List::class.java, ChatMessage::class.java)
    private val chatListAdapter = moshi.adapter<List<ChatMessage>>(chatListType)

    private val apiService: GeminiApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApiService::class.java)
    }

    fun serializeChat(messages: List<ChatMessage>): String {
        return chatListAdapter.toJson(messages)
    }

    fun deserializeChat(json: String?): List<ChatMessage> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            chatListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun chatTurn(
        history: List<ChatMessage>,
        systemInstruction: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Provide a graceful contextual response if API key is not configured in secrets yet
            return@withContext Result.success(
                "I am here! I'm analyzing your input. What specific nuance or concept would you like to master? (Tip: Add your GEMINI_API_KEY in the AI Studio Secrets panel for live model responses)."
            )
        }

        try {
            val contents = history.map { message ->
                val parts = mutableListOf<Part>()
                if (message.imageBase64 != null) {
                    parts.add(Part(inlineData = InlineData(mimeType = "image/jpeg", data = message.imageBase64)))
                }
                if (message.text.isNotBlank()) {
                    parts.add(Part(text = message.text))
                }
                Content(
                    parts = parts,
                    role = if (message.sender == "user") "user" else "model"
                )
            }

            val request = GenerateContentRequest(
                contents = contents,
                generationConfig = GenerationConfig(
                    thinkingConfig = ThinkingConfig(thinkingLevel = "low"),
                    temperature = 0.7f
                ),
                systemInstruction = systemInstruction?.let {
                    Content(parts = listOf(Part(text = it)))
                }
            )

            val response = apiService.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty response from Gemini"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateLinguisticScenario(history: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        val prompt = buildString {
            append("You are continuing an ongoing language tutoring conversation.\n")
            append("In the previous session, you and the student had the following conversation:\n")
            history.forEach {
                append("${if (it.sender == "user") "Student" else "Tutor"}: ${it.text}\n")
            }
            append("\nTASK:\n")
            append("Based on this conversation, extract the key linguistic element (grammatical structure or specific word/idiom) and generate an engaging, realistic everyday scenario where the student MUST use that element in response.\n")
            append("Make it feel like a natural continuation of your prior chat, not a test. Set up the situation vividly.\n")
            append("MANDATORY: End your prompt with the exact question: 'What would you say?'")
        }

        val system = "You are an expert language coach creating contextual speaking scenarios based on previous conversation notes. Always end with: 'What would you say?'"

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Intelligent fallback scenario based on recent chat topic
            val lastUserMsg = history.lastOrNull { it.sender == "user" }?.text ?: "this topic"
            return@withContext Result.success(
                "You find yourself discussing '$lastUserMsg' with a colleague who seems unsure about how to express it smoothly. They ask for your immediate thoughts. What would you say?"
            )
        }

        try {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)), role = "user")
                ),
                generationConfig = GenerationConfig(
                    thinkingConfig = ThinkingConfig(thinkingLevel = "low"),
                    temperature = 0.7f
                ),
                systemInstruction = Content(parts = listOf(Part(text = system)))
            )

            val response = apiService.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty scenario response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTechnicalQuestion(history: List<ChatMessage>): Result<String> = withContext(Dispatchers.IO) {
        val prompt = buildString {
            append("You are continuing an ongoing technical mentoring dialogue.\n")
            append("In the previous session, you and the engineer/student discussed:\n")
            history.forEach {
                append("${if (it.sender == "user") "Student" else "Mentor"}: ${it.text}\n")
            }
            append("\nTASK:\n")
            append("Extract the core technical concept, mechanism, component, or technique focused on in this discussion. Formulate a pointed, conversational technical question to test their recall and working knowledge.\n")
            append("Make it sound natural, like returning to a topic you discussed recently: e.g. 'Last time we spoke about... Can you explain how...?'")
        }

        val system = "You are a technical mentor reviewing previous engineering concepts with a student."

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            val lastUserMsg = history.lastOrNull { it.sender == "user" }?.text ?: "technical component"
            return@withContext Result.success(
                "Recall when we discussed '$lastUserMsg': How does this mechanism operate under standard conditions, and what is its primary role?"
            )
        }

        try {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(parts = listOf(Part(text = prompt)), role = "user")
                ),
                generationConfig = GenerationConfig(
                    thinkingConfig = ThinkingConfig(thinkingLevel = "low"),
                    temperature = 0.7f
                ),
                systemInstruction = Content(parts = listOf(Part(text = system)))
            )

            val response = apiService.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                Result.success(text)
            } else {
                Result.failure(Exception("Empty technical question response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun bitmapToBase64(bitmap: Bitmap): String {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        }
    }
}
