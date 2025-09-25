package com.example.sampleview.player

import android.os.Bundle
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.sampleview.AppLogger
import com.example.sampleview.R

class VideoViewActivity : AppCompatActivity(R.layout.activity_video_view) {

    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        videoView = findViewById(R.id.videoView)

        // 本地 raw 视频
        val videoUri = "android.resource://${packageName}/${R.raw.splash}".toUri()
        videoView.setVideoURI(videoUri)

        // 播放完成监听
        videoView.setOnCompletionListener {
            AppLogger.d("VideoViewActivity", "视频播放完成")
        }

        videoView.setOnPreparedListener { mediaPlayer ->
            val durationMs = mediaPlayer.duration
            AppLogger.d("VideoViewActivity", "视频总长度: $durationMs 毫秒")
            // 转成秒
            val durationSec = durationMs / 1000
            AppLogger.d("VideoViewActivity", "视频总长度: $durationSec 秒")
        }

        // 播放错误监听
        videoView.setOnErrorListener { _, what, extra ->
            AppLogger.d("VideoViewActivity", "视频播放错误")
            true
        }

        // 自动开始播放
        videoView.start()
    }

    override fun onStop() {
        super.onStop()
        videoView.stopPlayback()
    }
}