package com.example.sampleview.ad

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri

sealed class Ad {

    data class Image(
        val source: Source,
        val duration: Long = 3000L,
    ) : Ad()

    data class Video(
        val source: Source,
    ) : Ad()

    sealed class Source {
        data class Local(val path: String) : Source()
        data class Remote(val url: String) : Source()

        fun toUri(): Uri = when (this) {
            is Local -> path.toUri()
            is Remote -> url.toUri()
        }
    }

    companion object {
        fun imageLocalRes(context: Context, resId: Int, duration: Long = 3000L): Image {
            val path = "android.resource://${context.packageName}/$resId"
            return Image(Source.Local(path), duration)
        }

        fun imageRemote(url: String, duration: Long = 3000L): Image {
            return Image(Source.Remote(url), duration)
        }

        fun videoLocalRes(context: Context, resId: Int): Video {
            val path = "android.resource://${context.packageName}/$resId"
            return Video(Source.Local(path))
        }

        fun videoRemote(url: String): Video {
            return Video(Source.Remote(url))
        }
    }
}
