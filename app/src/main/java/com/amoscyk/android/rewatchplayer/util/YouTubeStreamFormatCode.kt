package com.amoscyk.android.rewatchplayer.util

object YouTubeStreamFormatCode {
    enum class Container {
        FLV, THREE_GP, MP4, WEBM, HLS, M4A
    }

    enum class Content {
        A, V, AV
    }

    data class StreamFormat(
        val itag: Int,
        val container: Container,
        val content: Content,
        val resolution: String? = null,
        val bitrate: String? = null
    )

    val MUX_FORMAT_MAP = mapOf(
        18 to "360p (MUX)",
        82 to "360p (MUX)",
        83 to "480p (MUX)",
        22 to "720p (MUX)",
        84 to "720p (MUX)",
        37 to "1080p (MUX)",
        85 to "1080p (MUX)",
        38 to "3072p (MUX)"
    )
    val ADAPTIVE_VIDEO_FORMAT_MAP = mapOf(
        160 to "144p",
        133 to "240p",
        134 to "360p",
        135 to "480p",
        136 to "720p",
        298 to "720p60",
        137 to "1080p",
        299 to "1080p60",
        263 to "1440p",
        138 to "2160p60",
        266 to "2160p60"
    )
    val ADAPTIVE_AUDIO_FORMAT_MAP = sortedMapOf(
        139 to "48k",
        140 to "128k",
        141 to "256k"
    )

    // NOT USED
    val FORMAT_CODES = sortedMapOf(
        5 to StreamFormat(5, Container.FLV, Content.AV, "240p"),
        6 to StreamFormat(6, Container.FLV, Content.AV, "270p"),
        17 to StreamFormat(17, Container.THREE_GP, Content.AV, "144p"),
        18 to StreamFormat(18, Container.MP4, Content.AV, "360p"),
        22 to StreamFormat(22, Container.MP4, Content.AV, "720p"),
        34 to StreamFormat(34, Container.FLV, Content.AV, "360p"),
        35 to StreamFormat(35, Container.FLV, Content.AV, "480p"),
        36 to StreamFormat(36, Container.THREE_GP, Content.AV, "180p"),
        37 to StreamFormat(37, Container.MP4, Content.AV, "1080p"),
        38 to StreamFormat(38, Container.MP4, Content.AV, "3072p"),
        43 to StreamFormat(43, Container.WEBM, Content.AV, "360p"),
        44 to StreamFormat(44, Container.WEBM, Content.AV, "480p"),
        45 to StreamFormat(45, Container.WEBM, Content.AV, "720p"),
        46 to StreamFormat(46, Container.WEBM, Content.AV, "1080p"),
        82 to StreamFormat(82, Container.MP4, Content.AV, "360p"),
        83 to StreamFormat(83, Container.MP4, Content.AV, "480p"),
        84 to StreamFormat(84, Container.MP4, Content.AV, "720p"),
        85 to StreamFormat(85, Container.MP4, Content.AV, "1080p"),
        92 to StreamFormat(92, Container.HLS, Content.AV, "240p"),
        93 to StreamFormat(93, Container.HLS, Content.AV, "360p"),
        94 to StreamFormat(94, Container.HLS, Content.AV, "480p"),
        95 to StreamFormat(95, Container.HLS, Content.AV, "720p"),
        96 to StreamFormat(96, Container.HLS, Content.AV, "1080p"),
        100 to StreamFormat(100, Container.WEBM, Content.AV, "360p"),
        101 to StreamFormat(101, Container.WEBM, Content.AV, "480p"),
        102 to StreamFormat(102, Container.WEBM, Content.AV, "720p"),
        132 to StreamFormat(132, Container.HLS, Content.AV, "240p"),
        133 to StreamFormat(133, Container.MP4, Content.V, "240p"),
        134 to StreamFormat(134, Container.MP4, Content.V, "360p"),
        135 to StreamFormat(135, Container.MP4, Content.V, "480p"),
        136 to StreamFormat(136, Container.MP4, Content.V, "720p"),
        137 to StreamFormat(137, Container.MP4, Content.V, "1080p"),
        138 to StreamFormat(138, Container.MP4, Content.V, "2160p60"),
        139 to StreamFormat(139, Container.M4A, Content.A, bitrate = "48k"),
        140 to StreamFormat(140, Container.M4A, Content.A, bitrate = "128k"),
        141 to StreamFormat(141, Container.M4A, Content.A, bitrate = "256k"),
        151 to StreamFormat(151, Container.HLS, Content.AV, "72p"),
        160 to StreamFormat(160, Container.MP4, Content.V, "144p"),
        167 to StreamFormat(167, Container.WEBM, Content.V, "360p"),
        168 to StreamFormat(168, Container.WEBM, Content.V, "480p"),
        169 to StreamFormat(169, Container.WEBM, Content.V, "1080p"),
        171 to StreamFormat(171, Container.WEBM, Content.A, bitrate = "128k"),
        218 to StreamFormat(218, Container.WEBM, Content.V, "480p"),
        219 to StreamFormat(219, Container.WEBM, Content.V, "144p"),
        242 to StreamFormat(242, Container.WEBM, Content.V, "240p"),
        243 to StreamFormat(243, Container.WEBM, Content.V, "360p"),
        244 to StreamFormat(244, Container.WEBM, Content.V, "480p"),
        245 to StreamFormat(245, Container.WEBM, Content.V, "480p"),
        246 to StreamFormat(246, Container.WEBM, Content.V, "480p"),
        247 to StreamFormat(247, Container.WEBM, Content.V, "720p"),
        248 to StreamFormat(248, Container.WEBM, Content.V, "1080p"),
        249 to StreamFormat(249, Container.WEBM, Content.A, bitrate = "50k"),
        250 to StreamFormat(250, Container.WEBM, Content.A, bitrate = "70k"),
        251 to StreamFormat(251, Container.WEBM, Content.A, bitrate = "160k"),
        264 to StreamFormat(264, Container.MP4, Content.V, "1440p"),
        266 to StreamFormat(266, Container.MP4, Content.V, "2160p60"),
        271 to StreamFormat(271, Container.WEBM, Content.V, "1440p"),
        272 to StreamFormat(272, Container.WEBM, Content.V, "4320p"),
        278 to StreamFormat(278, Container.WEBM, Content.V, "144p"),
        298 to StreamFormat(298, Container.MP4, Content.V, "720p60"),
        299 to StreamFormat(299, Container.MP4, Content.V, "1080p60"),
        302 to StreamFormat(302, Container.WEBM, Content.V, "720p60"),
        303 to StreamFormat(303, Container.WEBM, Content.V, "1080p60"),
        308 to StreamFormat(308, Container.WEBM, Content.V, "1440p60"),
        313 to StreamFormat(313, Container.WEBM, Content.V, "2160p"),
        315 to StreamFormat(315, Container.WEBM, Content.V, "2160p60"),
        330 to StreamFormat(330, Container.WEBM, Content.V, "144p60"),
        331 to StreamFormat(331, Container.WEBM, Content.V, "240p60"),
        332 to StreamFormat(332, Container.WEBM, Content.V, "360p60"),
        333 to StreamFormat(333, Container.WEBM, Content.V, "480p60"),
        334 to StreamFormat(334, Container.WEBM, Content.V, "720p60"),
        335 to StreamFormat(335, Container.WEBM, Content.V, "1080p60"),
        336 to StreamFormat(336, Container.WEBM, Content.V, "1440p60"),
        337 to StreamFormat(337, Container.WEBM, Content.V, "2160p60")
    )

    // NOT USED
    val MUXED_VIDEO_FORMATS = hashMapOf<Int, StreamFormat>()
    val ADAPTIVE_VIDEO_FORMATS = hashMapOf<Int, StreamFormat>()
    val ADAPTIVE_AUDIO_FORMATS = hashMapOf<Int, StreamFormat>()

//    init {
//        FORMAT_CODES.forEach { (idx, value) ->
//            when (value.container) {
//                YouTubeStreamFormatCode.Container.MP4 -> {
//                    if (value.content == YouTubeStreamFormatCode.Content.AV) {
//                        MUXED_VIDEO_FORMATS[idx] = value
//                    } else {
//                        ADAPTIVE_VIDEO_FORMATS[idx] = value
//                    }
//                }
//                YouTubeStreamFormatCode.Container.M4A -> {
//                    ADAPTIVE_AUDIO_FORMATS[idx] = value
//                }
//                else -> {
//                }
//            }
//        }
//    }
}



