package com.example.sampleview

import android.app.Application
import android.content.Context
import com.example.sampleview.crash.JavaCrashHandler
import com.example.sampleview.eventtracker.EventTracker
import com.example.sampleview.log.JsonFormatTree
import com.example.sampleview.log.Timber

class SampleApp : Application() {
    private val TAG = "SampleApp"

    companion object {
        lateinit var instance: SampleApp
            private set
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        EventTracker.init(this)
        AppActivityManager.init(this)
        Timber.plant(JsonFormatTree())
        JavaCrashHandler.init(this)
//        XCrash.init(this)
        instance = this

    }

}