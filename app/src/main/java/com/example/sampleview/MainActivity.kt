package com.example.sampleview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.QuickViewHolder
import com.example.sampleview.behavior.BehaviorActivity
import com.example.sampleview.behavior.BottomSheetActivity
import com.example.sampleview.crash.TestCrashActivity
import com.example.sampleview.edit.EditActivity
import com.example.sampleview.eventtracker.EventTrackActivity
import com.example.sampleview.file.FileActivity
import com.example.sampleview.flexbox.FlexboxActivity
import com.example.sampleview.flow.FlowBusTestActivity
import com.example.sampleview.glide.GlideActivity
import com.example.sampleview.html.HtmlActivity
import com.example.sampleview.loading.LoadingActivity
import com.example.sampleview.log.LogActivity
import com.example.sampleview.mvi.ui.LoginActivity
import com.example.sampleview.permission.PermissionActivity
import com.example.sampleview.popupmanager.PopupManagerActivity
import com.example.sampleview.reddot.RedDotTestActivity
import com.example.sampleview.taskScheduler.test.TaskActivity
import com.example.sampleview.taskflowengine.TaskFlowActivity
import com.example.sampleview.trip.TripInfoActivity
import com.example.sampleview.voc.VocActivity
import com.example.sampleview.websocket.WebSocketActivity


class MainActivity : AppCompatActivity() {
    private val classArray = listOf(
        VocActivity::class.java,
        EventTrackActivity::class.java,
        TaskFlowActivity::class.java,
        PopupManagerActivity::class.java,
        LoginActivity::class.java,
        HtmlActivity::class.java,
        RedDotTestActivity::class.java,
        TripInfoActivity::class.java,
        BottomSheetActivity::class.java,
        TestCrashActivity::class.java,
        GlideActivity::class.java,
        LoadingActivity::class.java,
        WebSocketActivity::class.java,
        LogActivity::class.java,
        FlowBusTestActivity::class.java,
        BehaviorActivity::class.java,
        FileActivity::class.java,
        TaskActivity::class.java,
        EditActivity::class.java,
        TimeActivity::class.java,
        FlexboxActivity::class.java,
        PermissionActivity::class.java,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView = findViewById<RecyclerView>(R.id.rvList)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = object : BaseQuickAdapter<String, QuickViewHolder>(classArray.map { it.simpleName }) {
            override fun onBindViewHolder(
                holder: QuickViewHolder, position: Int, item: String?,
            ) {
                holder.getView<TextView>(R.id.tvExample).text = classArray[position].simpleName
            }

            override fun onCreateViewHolder(
                context: Context, parent: ViewGroup, viewType: Int,
            ): QuickViewHolder {
                return QuickViewHolder(R.layout.cell_example_layout, parent)
            }
        }.apply {
            setOnItemClickListener(object : BaseQuickAdapter.OnItemClickListener<String> {
                override fun onClick(
                    adapter: BaseQuickAdapter<String, *>, view: View, position: Int,
                ) {
                    startActivity(Intent(this@MainActivity, classArray[position]))
                }

            })
        }
    }
}