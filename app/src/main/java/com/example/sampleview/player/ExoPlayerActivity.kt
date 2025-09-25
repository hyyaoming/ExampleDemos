package com.example.sampleview.player

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.sampleview.AppLogger
import com.example.sampleview.R

class ExoPlayerActivity : AppCompatActivity(R.layout.activity_exo_player) {

    private lateinit var playerView: PlayerView
    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerView = findViewById(R.id.playerView)
        initializePlayer()
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build()
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val videoDuration = player?.duration ?: 0
                    AppLogger.d("ExoPlayerActivity", "视频时长为:$videoDuration")
                }
            }
        })
        playerView.player = player

        // raw 目录视频 URI
        val videoUri = "android.resource://${packageName}/${R.raw.splash}".toUri()
        val mediaItem = MediaItem.fromUri(videoUri)

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.playWhenReady = true
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

}