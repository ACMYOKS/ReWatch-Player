package com.amoscyk.android.rewatchplayer.ytextractor

import android.util.Log
import com.amoscyk.android.rewatchplayer.util.TimeLogger
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

class YouTubeExtractor(private val youTubeOpenService: YouTubeOpenService) {

    suspend fun extractInfo(videoId: String): YTInfo? =
        withContext(Dispatchers.IO) {
            val log = TimeLogger(TAG)
            val response = youTubeOpenService.getWebHtml(videoId).execute()
            log.addKnot("get response")
            val playerScript = response.body()?.string()?.let { getPlayerScript(it) } ?: return@withContext null
            log.addKnot("parse script")
            Log.d(TAG, playerScript)
            val json = getPlayerResponse(playerScript)
            log.addKnot("get player response")
            val data = getSteamingDataFromJson(json)
            log.addKnot("get streaming data from json")
            log.dumpToLog()
            return@withContext YTInfo(
                data.associateBy({it.itag}, {it.url}),
                getAvailableFormatList(data.map { it.itag })
            )
        }

    private fun getPlayerScript(rawHtml: String): String? {
        // NOTE: the following code presumes that the player script must contain term 'ytplayer'
        // and it must be minified into one line.
        // TODO: find a better parsing method to get the content of the script
        val matches = Regex(""".*ytplayer.*\n""").find(rawHtml)
        // find the only first script that contains ytplayer and remove <script> tag
        return matches?.value?.replace(Regex("""<\s?/?\s?script\s?>""",
            RegexOption.IGNORE_CASE), "")
    }

    private fun getPlayerResponse(playerScript: String): String {
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
        return playerScript.substring(startIndex+1, endIndex).removeEscapeChar()
    }

    private fun getSteamingDataFromJson(jsonString: String): List<ResourceFormat> {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(PlayerResponse::class.java)
        val playerResponse = adapter.fromJson(jsonString)
        playerResponse?.streamingData?.formats?.forEach {
            Log.d(TAG, "resource: ${it.itag} ${it.mimeType} ${it.url}")
        }
        playerResponse?.streamingData?.adaptiveFormats?.forEach {
            Log.d(TAG, "adaptive format resource: ${it.itag} ${it.mimeType} ${it.url}")
        }
        return playerResponse?.streamingData?.formats.orEmpty() +
                playerResponse?.streamingData?.adaptiveFormats.orEmpty()
    }

    private fun getAvailableFormatMap(itags: List<Int>): Map<Int, YouTubeStreamFormatCode.StreamFormat> {
        return YouTubeStreamFormatCode.FORMAT_CODES.filterKeys { it in itags }
    }

    private fun getAvailableFormatList(itags: List<Int>): List<YouTubeStreamFormatCode.StreamFormat> {
        return YouTubeStreamFormatCode.FORMAT_CODES.mapNotNull { if (it.key in itags) it.value else null }
    }

    private fun String.removeEscapeChar(): String {
        return this.replace(Regex("""\\(["'\\/a-zA-Z])""")) { result: MatchResult ->
            result.groups[1]?.value.orEmpty()
        }
    }

    data class YTInfo(
        val urlMap: Map<Int, String>,
        val availableFormats: List<YouTubeStreamFormatCode.StreamFormat>
    )

    companion object {
        const val BASE_URL = "https://www.youtube.com"
        const val TAG = "YouTubeExtractor"
    }

}