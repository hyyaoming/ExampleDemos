package com.example.sampleview.loading

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.sampleview.R

class LoadingActivity : AppCompatActivity(R.layout.activity_loading) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btnLoading).setOnClickListener {
            TipDialog.Builder(this).setTipWord("提交中").create().show()
        }
    }

}