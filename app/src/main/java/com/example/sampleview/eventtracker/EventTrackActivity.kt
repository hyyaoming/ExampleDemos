package com.example.sampleview.eventtracker

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.sampleview.R
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.UploadMode
import java.util.UUID

class EventTrackActivity : AppCompatActivity(R.layout.activity_event_track) {
    private lateinit var btnAddEvent: Button
    private lateinit var btnFlushEvent: Button
    private lateinit var btnNormalEvent: Button
    private lateinit var btnNormalResetEvent: Button
    private lateinit var btnColdLaunchTimeEvent: Button
    private lateinit var btnColdLaunchTimeResetEvent: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        btnAddEvent = findViewById<Button>(R.id.btnAddEvent)
        btnFlushEvent = findViewById<Button>(R.id.btnFlushEvent)
        btnNormalEvent = findViewById<Button>(R.id.btnNormalEvent)
        btnNormalResetEvent = findViewById<Button>(R.id.btnNormalResetEvent)
        btnColdLaunchTimeEvent = findViewById<Button>(R.id.btnColdLaunchTimeEvent)
        btnColdLaunchTimeResetEvent = findViewById<Button>(R.id.btnColdLaunchTimeResetEvent)

        EventDurationTracker.start("EventTrackActivity1")
        EventDurationTracker.start("EventTrackActivity2")

        btnAddEvent.setOnClickListener {
            val event = Event.Builder("click:${UUID.randomUUID()}").uploadMode(UploadMode.BATCH).build()
            EventTracker.track(event)
        }

        btnFlushEvent.setOnClickListener {
            EventTracker.flushAll()
        }

        btnNormalEvent.setOnClickListener {
            val elapsed = EventDurationTracker.stop("EventTrackActivity1")
            println("耗时:$elapsed")
        }

        btnNormalResetEvent.setOnClickListener {
            val elapsed = EventDurationTracker.stop("EventTrackActivity2", true)
            println("耗时:$elapsed")
        }

        btnColdLaunchTimeEvent.setOnClickListener {
            val elapsed = EventDurationTracker.stopOrFromCold("fromColdLaunchEvent")
            println("耗时:$elapsed")
        }

        btnColdLaunchTimeResetEvent.setOnClickListener {
            val elapsed = EventDurationTracker.stopOrFromCold("fromColdLaunchEventAndReset")
            println("耗时:$elapsed")
        }

    }

}