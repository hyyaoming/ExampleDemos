package com.example.sampleview.ad

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.appcompat.widget.AppCompatImageView
import androidx.media3.common.C.TIME_UNSET
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.example.sampleview.dp2px

class AdView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = -1,
) : FrameLayout(context, attrs, defStyleAttr) {

    private var finished = false
    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var countdownView: AdCountDownView? = null
    private var listener: AdListener? = null

    fun loadAd(ad: Ad, listener: AdListener) {
        this.listener = listener
        removeAllViews()
        val display = createDisplay(ad)
        display.display(this@AdView, listener)
    }

    private fun cleanup() {
        exoPlayer?.apply {
            playWhenReady = false
            stop()
            release()
            removeListener(playerBackListener)
        }
        exoPlayer = null
        playerView?.adViewGroup?.removeAllViews()
        playerView?.player = null
        playerView = null
        countdownView?.cancelCountDown()
        countdownView = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cleanup()
    }

    private fun initCountdownText(duration: Long) {
        countdownView = AdCountDownView(context).apply {
            setOnClickListener { finishAd() }
        }
        val size = dp2px(48f)
        val params = LayoutParams(size, size)
        params.gravity = Gravity.TOP or Gravity.END
        params.setMargins(0, dp2px(28f), dp2px(28f), 0)
        addView(countdownView, params)
        startCountdown(duration)
    }

    private fun startCountdown(duration: Long) {
        countdownView?.startCountDown(duration) { finishAd() }
    }

    private fun finishAd() {
        if (finished) return
        finished = true
        cleanup()
        listener?.onFinish()
        listener = null
    }

    private val playerBackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                exoPlayer?.apply {
                    playWhenReady = true
                    playerView?.animate()?.alpha(1f)?.setDuration(200)?.start()
                    val durationMs = if (duration == TIME_UNSET.toLong()) 5000 else duration
                    initCountdownText(durationMs)
                }
            }
            if (state == Player.STATE_ENDED) {
                finishAd()
            }
        }
    }

    private fun createDisplay(ad: Ad): AdDisplay = when (ad) {
        is Ad.Image -> object : AdDisplay {
            override fun display(container: FrameLayout, listener: AdListener) {
                val uri = ad.source.toUri()
                val imageView = AppCompatImageView(container.context).apply {
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    setOnClickListener { listener.onClick() }
                }
                container.addView(imageView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                Glide.with(container.context).load(uri).into(imageView)
                listener.onExposure()
                initCountdownText(ad.duration)
            }
        }
        is Ad.Video -> object : AdDisplay {
            @OptIn(UnstableApi::class)
            override fun display(container: FrameLayout, listener: AdListener) {
                playerView = PlayerView(context).apply {
                    useController = false
                    this.alpha = 0f
                    setShutterBackgroundColor(Color.TRANSPARENT)
                    setKeepContentOnPlayerReset(true)
                }
                container.addView(playerView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

                exoPlayer = ExoPlayer.Builder(container.context).build().apply {
                    val uri = ad.source.toUri()
                    playerView?.player = this
                    setMediaItem(MediaItem.fromUri(uri))
                    playWhenReady = false
                    prepare()
                    addListener(playerBackListener)
                }

                playerView?.setOnClickListener { listener.onClick() }
                listener.onExposure()
            }
        }
    }
}
