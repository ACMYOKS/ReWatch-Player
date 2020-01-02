package com.amoscyk.android.rewatchplayer.ytextractor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class YouTubeExtractor {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(ScalarsConverterFactory.create())
        .build()
    private val ytService = retrofit.create(YouTubeFileService::class.java)

    suspend fun extractInfo(videoId: String) {
        withContext(Dispatchers.IO) {
            val response = ytService.getWebHtml(videoId).execute()
            val playerScript = response.body()?.string()?.let { getPlayerScript(it) } ?: return@withContext
            Log.d("AMOS", playerScript)
            val json = getPlayerResponseJson(playerScript)
            json.getJSONObject("streamingData")
        }
    }

    private fun getPlayerScript(rawHtml: String): String? {
        // NOTE: the following code presumes that the player script must contain term 'ytplayer'
        // and it must be minified into one line.
        // TODO: find a better parsing method to get the content of the script
        val matches = Regex(""".*ytplayer.*\n""").findAll(rawHtml)
        // find the only first script that contains ytplayer and remove <script> tag
        return matches.firstOrNull()?.value?.replace(Regex("""<\s?/?\s?script\s?>""",
            RegexOption.IGNORE_CASE), "")
    }

    private fun getPlayerResponseJson(playerScript: String): JSONObject {
        // retrieve player_response field from json, which is also a json object
        val targetStr = "\"player_response\""
        var startIndex = playerScript.indexOf(targetStr) + targetStr.length
        while (startIndex < playerScript.length && playerScript[startIndex] != '"') ++startIndex
        var endIndex = startIndex
        while (endIndex < playerScript.length) {
            ++endIndex
            if (playerScript[endIndex] == '}' &&
                endIndex + 1 < playerScript.length && playerScript[endIndex+1] == '"') {
                ++endIndex
                break
            }
        }
        val jsonStr = playerScript.substring(startIndex+1, endIndex).removeEscapeChar()
        return JSONObject(jsonStr)
    }

    private fun String.removeEscapeChar(): String {
        return this.replace(Regex("""\\(["'\\/a-zA-Z])""")) { result: MatchResult ->
            result.groups[1]?.value.orEmpty()
        }
    }

    companion object {
        const val BASE_URL = "https://www.youtube.com"
    }

}