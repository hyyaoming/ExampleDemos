package com.example.sampleview.behavior

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sampleview.R
import com.google.android.material.bottomsheet.BottomSheetBehavior

class BehaviorActivity : AppCompatActivity(R.layout.activity_behavior) {

    private lateinit var behavior: BottomSheetBehavior<LinearLayout>
    private lateinit var mapContainer: View
    private lateinit var sheet: LinearLayout
    private var velocityTracker: VelocityTracker? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mapContainer = findViewById(R.id.map_container)
        sheet = findViewById(R.id.bottom_sheet)
        behavior = BottomSheetBehavior.from(sheet).apply {
            isHideable = false
            isFitToContents = false
            halfExpandedRatio = 0.5f
            peekHeight = resources.getDimensionPixelSize(R.dimen.peek_height)
        }

        // RecyclerView 简单填充数据
        findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(this@BehaviorActivity)
            adapter = Adapter((1..50).map { "Item $it" })
        }

        // 地图视差联动
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(sheet: View, slideOffset: Float) {
                mapContainer.translationY = -slideOffset * 300f
            }

            override fun onStateChanged(sheet: View, newState: Int) {}
        })

        // 惯性吸附
//        sheet.setOnTouchListener { _, event ->
//            if (velocityTracker == null) velocityTracker = VelocityTracker.obtain()
//            velocityTracker?.addMovement(event)
//
//            if (event.action == MotionEvent.ACTION_UP) {
//                velocityTracker?.apply {
//                    computeCurrentVelocity(1000)
//                    val yVel = yVelocity
//                    when {
//                        yVel < -1500 -> behavior.state = BottomSheetBehavior.STATE_EXPANDED
//                        yVel > 1500 -> behavior.state = BottomSheetBehavior.STATE_COLLAPSED
//                        else -> {
//                            val top = sheet.top.toFloat()
//                            val height = sheet.height.toFloat()
//                            val fraction = 1f - top / (height - behavior.peekHeight)
//                            behavior.state = when {
//                                fraction < 0.33f -> BottomSheetBehavior.STATE_COLLAPSED
//                                fraction < 0.66f -> BottomSheetBehavior.STATE_HALF_EXPANDED
//                                else -> BottomSheetBehavior.STATE_EXPANDED
//                            }
//                        }
//                    }
//                    clear()
//                }
//                velocityTracker?.recycle()
//                velocityTracker = null
//            }
//            false
//        }
    }

    inner class Adapter(private val items: List<String>) : RecyclerView.Adapter<Adapter.VH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.text.text = items[position]
        }

        override fun getItemCount() = items.size

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val text: TextView = v.findViewById(android.R.id.text1)
        }
    }
}