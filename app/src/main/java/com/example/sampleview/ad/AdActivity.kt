package com.example.sampleview.ad

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.sampleview.AppLogger
import com.example.sampleview.R

class AdActivity : AppCompatActivity(R.layout.activity_ad) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val adView = findViewById<AdView>(R.id.adView)

//        val videoAd = Ad.videoLocalRes(this, R.raw.splash)
        val imageAd = Ad.imageLocalRes(this, R.mipmap.ad_img)
        adView.loadAd(imageAd, object : AdListener {
            override fun onClick() {
                AppLogger.d("AdActivity", "广告点击")
            }

            override fun onExposure() {
                AppLogger.d("AdActivity", "广告曝光")
            }

            override fun onFinish() {
                AppLogger.d("AdActivity", "广告结束")
            }
        })
    }

}