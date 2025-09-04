package com.example.sampleview.voc

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.AppLogger
import com.example.sampleview.R
import com.example.sampleview.voc.core.VocMediator
import com.example.sampleview.voc.data.model.VocScene
import kotlinx.coroutines.launch

class VocActivity : AppCompatActivity(R.layout.activity_voc) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btnRequestVoc).setOnClickListener {
            lifecycleScope.launch {
                val vocResult = VocMediator().requestVoc(this@VocActivity, VocScene.CANCEL_ORDER)
                AppLogger.d("VocActivity","VocActivity.vocResult:${vocResult}")
            }
        }
    }

}