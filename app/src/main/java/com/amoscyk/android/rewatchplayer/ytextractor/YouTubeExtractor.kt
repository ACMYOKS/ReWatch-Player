package com.amoscyk.android.rewatchplayer.ytextractor

import android.util.Log
import com.amoscyk.android.rewatchplayer.datasource.vo.AvailableStreamFormat
import com.amoscyk.android.rewatchplayer.datasource.vo.RPThumbnailDetails
import com.amoscyk.android.rewatchplayer.util.TimeLogger
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class YouTubeExtractor(private val youTubeOpenService: YouTubeOpenService) {

    private val playerResponseAdapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(PlayerResponse::class.java)

    suspend fun extractInfo(videoId: String): YTInfo? =
        withContext(Dispatchers.IO) {
            val json = getJsonFromInfoApi(videoId) ?: return@withContext null
            val log = TimeLogger(TAG)
            log.addKnot("get streaming data from json")
            log.dumpToLog()
            val resObj = playerResponseAdapter.fromJson(json) ?: return@withContext null
            val muxedNonEmpty = resObj.streamingData.formats.filter { it.url.isNotBlank() }
            val adaptiveNonEmpty = resObj.streamingData.adaptiveFormats.filter { it.url.isNotBlank() }
            val formats = muxedNonEmpty + adaptiveNonEmpty
            val muxedTag = muxedNonEmpty.map { it.itag }
            val adaptiveTag = adaptiveNonEmpty.map { it.itag }
            return@withContext YTInfo(
                resObj.videoDetails,
                formats.associateBy({it.itag}, {it.url}),
                AvailableStreamFormat(
                    YouTubeStreamFormatCode.MUXED_VIDEO_FORMATS.filterKeys { muxedTag.contains(it) },
                    YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMATS.filterKeys { adaptiveTag.contains(it) },
                    YouTubeStreamFormatCode.ADAPTIVE_AUDIO_FORMATS.filterKeys { adaptiveTag.contains(it) })
                )
        }

    suspend fun getThumnails(videoId: String): RPThumbnailDetails =
        withContext(Dispatchers.IO) {

            return@withContext RPThumbnailDetails(

            )
        }

    private suspend fun getJsonFromWebHtml(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            val log = TimeLogger(TAG)
            val response = youTubeOpenService.getWebHtml(videoId).execute()
            log.addKnot("get response")
            val playerScript = response.body()?.string()?.let { getPlayerScript(it) } ?: return@withContext null
            log.addKnot("parse script")
            Log.d(TAG, playerScript)
            val json = getPlayerResponse(playerScript)
            log.addKnot("get player response")
            log.dumpToLog()
            return@withContext json
        }
    }

    private suspend fun getJsonFromInfoApi(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            val log = TimeLogger(TAG)
            val response = youTubeOpenService.getVideoInfo(videoId).execute()
            log.addKnot("get response")
            val json = response.body()?.string()?.let { getPlayerResponse2(it) } ?: return@withContext null
            Log.d(TAG, json)
            log.addKnot("get player response")
            log.dumpToLog()
            return@withContext json
        }
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

    private fun getPlayerResponse2(infoResponse: String): String? {
        val fieldName = "player_response="
        val startIdx = infoResponse.indexOf(fieldName).let {
            if (it == -1) return null
            it
        } + fieldName.length
        val endIdx = infoResponse.indexOf('&', startIdx).let {
            if (it == -1) infoResponse.lastIndex
            else it
        }
        val substr = infoResponse.substring(startIdx, endIdx)
        Log.d(TAG, infoResponse)
        Log.d(TAG, substr)
        try {
            val jsonStr = URLDecoder.decode(substr, StandardCharsets.UTF_8.name())
            return jsonStr
        } catch (e: Exception) {
            return null
        }
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
        val videoDetails: VideoDetails,
        val urlMap: Map<Int, String>,
        val availableStreamFormat: AvailableStreamFormat
    )

    companion object {
        const val BASE_URL = "https://www.youtube.com"
        const val TAG = "YouTubeExtractor"
    }

}