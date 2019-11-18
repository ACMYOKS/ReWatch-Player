package com.amoscyk.android.rewatchplayer.datasource.vo

import android.os.Parcelable
import com.google.api.services.youtube.model.Thumbnail
import kotlinx.android.parcel.Parcelize

@Parcelize
data class RPThumbnail(
    val url: String,
    val width: Long,
    val height: Long
): Parcelable {
    companion object {
        fun fromApi(thumbnail: Thumbnail?): RPThumbnail? {
            if (thumbnail == null) return null
            return RPThumbnail(
                thumbnail.url,
                thumbnail.width,
                thumbnail.height
            )
        }
    }
}