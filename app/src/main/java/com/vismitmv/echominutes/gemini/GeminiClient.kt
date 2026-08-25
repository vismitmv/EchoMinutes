package com.vismitmv.echominutes.gemini

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

private val json = Json { ignoreUnknownKeys = true }
private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .build()

private const val MODEL = "gemini-3.6-flash"
private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
private const val MAX_FILE_BYTES = 19 * 1024 * 1024 // 19 MB safety limit

@Serializable
private data class InlineData(val mimeType: String, val data: String)

@Serializable
private data class Part(val text: String? = null, val inlineData: InlineData? = null)

@Serializable
private data class Content(val parts: List<Part>, val role: String = "user")

@Serializable
private data class GenerateRequest(val contents: List<Content>)

@Serializable
private data class Candidate(val content: Content)

@Serializable
private data class GenerateResponse(val candidates: List<Candidate> = emptyList())

data class GeminiResult(val transcript: String, val summary: String)

object GeminiClient {

    private val PROMPT = """
You are a multilingual meeting assistant. The attached audio is from an in-person meeting that may contain speech in English, Hindi, or other Indian languages.

Please do the following:
1. **Transcribe** the full meeting conversation. Where multiple speakers are evident, label them Speaker 1, Speaker 2, etc. Preserve the original language of each speaker's words, adding an English translation in parentheses for non-English segments.
2. **Summarize** the key discussion points, decisions made, and any action items in clear, concise English bullet points.

Respond strictly in the following format:

## Transcript
[Full transcription here]

## Summary
[Bullet-point summary here]
    """.trimIndent()

    suspend fun transcribeAndSummarize(audioFile: File, apiKey: String): Result<GeminiResult> {
        return try {
            if (audioFile.length() > MAX_FILE_BYTES) {
                return Result.failure(Exception("Audio file is too large (>${MAX_FILE_BYTES / 1024 / 1024} MB). Please record shorter meetings."))
            }

            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.getEncoder().encodeToString(audioBytes)

            val requestBody = GenerateRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(inlineData = InlineData(mimeType = "audio/mp4", data = base64Audio)),
                            Part(text = PROMPT)
                        )
                    )
                )
            )

            val bodyJson = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: return Result.failure(Exception("Empty response from Gemini API"))

            if (!response.isSuccessful) {
                return Result.failure(Exception("Gemini API error ${response.code}: $responseBody"))
            }

            val parsed = json.decodeFromString<GenerateResponse>(responseBody)
            val text = parsed.candidates.firstOrNull()?.content?.parts
                ?.firstOrNull { it.text != null }?.text
                ?: return Result.failure(Exception("No text in Gemini response"))

            Result.success(parseGeminiOutput(text))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseGeminiOutput(text: String): GeminiResult {
        val transcriptMatch = Regex("## Transcript\\n([\\s\\S]*?)(?=## Summary|$)").find(text)
        val summaryMatch = Regex("## Summary\\n([\\s\\S]*)").find(text)
        val transcript = transcriptMatch?.groupValues?.get(1)?.trim() ?: text
        val summary = summaryMatch?.groupValues?.get(1)?.trim() ?: ""
        return GeminiResult(transcript = transcript, summary = summary)
    }
}
