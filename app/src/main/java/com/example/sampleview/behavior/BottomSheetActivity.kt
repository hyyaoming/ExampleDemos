package com.example.sampleview.behavior

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sampleview.R
import com.example.sampleview.dp2px
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout

sealed class ListItem {
    data class Header(val title: String) : ListItem()
    data class Content(val text: String) : ListItem()
}

class BottomSheetActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout

    private val groupTitles = listOf("分组1", "分组2", "分组3")
    private val items = mutableListOf<ListItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bottom_sheet)

        tabLayout = findViewById(R.id.tab_layout)
        recyclerView = findViewById(R.id.recycler_view)
        val bottomSheet = findViewById<View>(R.id.bottom_sheet)

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = dp2px(200f)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        // 准备数据：每组一个 Header，3个内容项
        groupTitles.forEach { title ->
            items.add(ListItem.Header(title))
            for (i in 1..3) {
                items.add(ListItem.Content("$title 的内容项 $i"))
            }
        }

        // TabLayout 初始化 Tab
        groupTitles.forEach { title ->
            tabLayout.addTab(tabLayout.newTab().setText(title))
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = GroupAdapter(items)
        recyclerView.adapter = adapter

        // Tab 点击事件：滚动对应组头并自动展开 BottomSheet（如果需要）
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val groupIndex = tab.position
                val headerPosition = findHeaderPositionForGroup(groupIndex)

                // 找到该分组下的第一个内容项的位置
                val targetPosition = headerPosition + 1
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager

                // 滚动后该项应该出现在顶部
                layoutManager.scrollToPositionWithOffset(targetPosition, 0)

                recyclerView.post {
                    val firstVisible = layoutManager.findFirstVisibleItemPosition()
                    if (firstVisible != targetPosition && bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    }
                }
            }


            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    // 找到对应分组Header在items列表中的位置
    private fun findHeaderPositionForGroup(groupIndex: Int): Int {
        var count = -1
        items.forEachIndexed { index, item ->
            if (item is ListItem.Header) {
                count++
                if (count == groupIndex) return index
            }
        }
        return 0
    }
}

class GroupAdapter(private val items: List<ListItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.Content -> TYPE_CONTENT
            else -> throw IllegalArgumentException("未知的item类型")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            ContentViewHolder(view)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ListItem.Content -> (holder as ContentViewHolder).bind(item)
            else -> throw IllegalArgumentException("未知的item类型")
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv = itemView.findViewById<TextView>(android.R.id.text1)
        fun bind(item: ListItem.Header) {
            tv.text = item.title
            tv.setBackgroundColor(0xFFE0E0E0.toInt()) // 灰色背景区分组头
        }
    }

    class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv1 = itemView.findViewById<TextView>(android.R.id.text1)
        private val tv2 = itemView.findViewById<TextView>(android.R.id.text2)
        fun bind(item: ListItem.Content) {
            tv1.text = item.text
            tv2.text = "详情"
        }
    }
}
