package com.amoscyk.android.rewatchplayer.util

import com.amoscyk.android.rewatchplayer.AppConstant
import com.amoscyk.android.rewatchplayer.datasource.vo.RPThumbnail
import com.amoscyk.android.rewatchplayer.datasource.vo.RPThumbnailDetails

object YouTubeThumbnailHelper {
    private const val DEFAULT = AppConstant.YOUTUBE_IMG_API_URL + "/%s/default.jpg"
    private const val HQ_DEFAULT = AppConstant.YOUTUBE_IMG_API_URL + "/%s/hqdefault.jpg"
    private const val MQ_DEFAULT = AppConstant.YOUTUBE_IMG_API_URL + "/%s/mqdefault.jpg"
    private const val SD_DEFAULT = AppConstant.YOUTUBE_IMG_API_URL + "/%s/sddefault.jpg"
    private const val MAX_RES_DEFAULT = AppConstant.YOUTUBE_IMG_API_URL + "/%s/maxresdefault.jpg"

    fun getThumbnail(videoId: String) = RPThumbnailDetails(
        default = RPThumbnail(getDefaultUrl(videoId)),
        high = RPThumbnail(getHighUrl(videoId)),
        medium = RPThumbnail(getMediumUrl(videoId)),
        standard = RPThumbnail(getStandardUrl(videoId)),
        maxres = RPThumbnail(getMaxResUrl(videoId))
    )

    fun getDefaultUrl(videoId: String) = DEFAULT.format(videoId)
    fun getHighUrl(videoId: String) = HQ_DEFAULT.format(videoId)
    fun getMediumUrl(videoId: String) = MQ_DEFAULT.format(videoId)
    fun getStandardUrl(videoId: String) = SD_DEFAULT.format(videoId)
    fun getMaxResUrl(videoId: String) = MAX_RES_DEFAULT.format(videoId)
}