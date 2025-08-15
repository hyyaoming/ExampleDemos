package com.example.sampleview.log

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.BackoffPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.sampleview.R
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class LogActivity : AppCompatActivity(R.layout.activity_log) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filterChain = FilterChain()
        filterChain.addFilter(LevelFilter("DEBUG"))
        LogManager.initialize(this) {
            filter { filterChain }
            writer { FileLogWriter(this@LogActivity) }
        }

        findViewById<Button>(R.id.btnPrintLog).setOnClickListener {
            Timber.tag("LogActivity").d("""{"code":200,"msg":"","traceId":null,"data":{"count":0,"pollTime":10000}}""")
        }

        findViewById<Button>(R.id.btnWriteLog).setOnClickListener {
            LogManager.get().log("DEBUG", "LogActivity", "Button clicked, write log.")
        }

        findViewById<Button>(R.id.btnFlushLog).setOnClickListener {
            // 强制flush缓存
            lifecycleScope.launch {
                LogManager.get().flush()
            }
        }

        findViewById<Button>(R.id.btnTriggerUpload).setOnClickListener {
            scheduleUploadWorker()
        }
    }

    private fun scheduleUploadWorker() {
        val workRequest = OneTimeWorkRequestBuilder<UploadWorker>()
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10, TimeUnit.SECONDS
            )
            .build()
        WorkManager.getInstance(this).enqueue(workRequest)
    }
}
