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
): Parcelable

fun ThumbnailDetails?.toRPThumbnailDetails(): RPThumbnailDetails {
    if (this == null) return RPThumbnailDetails()
    return RPThumbnailDetails(
        default.toRPThumbnail(),
        high.toRPThumbnail(),
        maxres.toRPThumbnail(),
        medium.toRPThumbnail(),
        standard.toRPThumbnail()
    )
}