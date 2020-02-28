package com.amoscyk.android.rewatchplayer.datasource.vo

import android.os.Parcelable
import com.google.api.services.youtube.model.ThumbnailDetails
import kotlinx.android.parcel.Parcelize
import java.io.Serializable

@Parcelize
data class RPThumbnailDetails(
    val default: RPThumbnail? = null,
    val high: RPThumbnail? = null,
    val maxres: RPThumbnail? = null,
    val medium: RPThumbnail? = null,
    val standard: RPThumbnail? = null
): Parcelable {
    companion object {
        fun fromApi(thumbnails: ThumbnailDetails): RPThumbnailDetails {
            return RPThumbnailDetails(
                RPThumbnail.fromApi(thumbnails.default),
                RPThumbnail.fromApi(thumbnails.high),
                RPThumbnail.fromApi(thumbnails.maxres),
                RPThumbnail.fromApi(thumbnails.medium),
                RPThumbnail.fromApi(thumbnails.standard)
            )
        }
    }
}