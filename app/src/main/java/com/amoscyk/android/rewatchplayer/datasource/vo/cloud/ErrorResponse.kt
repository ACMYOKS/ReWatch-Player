package com.amoscyk.android.rewatchplayer.datasource.vo.cloud

import com.amoscyk.android.rewatchplayer.datasource.vo.InvalidArgumentException
import com.amoscyk.android.rewatchplayer.datasource.vo.NoSuchVideoIdException
import com.amoscyk.android.rewatchplayer.datasource.vo.ServerErrorException
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.*

@JsonClass(generateAdapter = true)
class ErrorResponse(
    @field:Json(name = "statusCode") val statusCode: Int,
    @field:Json(name = "message") val message: String?
) {
    fun getException(): Exception {
        return when (statusCode) {
            in 400 until 500 -> {
                val s = message.orEmpty().toLowerCase(Locale.getDefault())
                when {
                    s.contains("no such video id") -> NoSuchVideoIdException(
                        message
                    )
                    s.contains("abscence of argument") -> InvalidArgumentException(
                        message
                    )
                    else -> Exception(message)
                }
            }
            in 500 until 600 -> {
                ServerErrorException(
                    message
                )
            }
            else -> {
                Exception(message)
            }
        }
    }
}