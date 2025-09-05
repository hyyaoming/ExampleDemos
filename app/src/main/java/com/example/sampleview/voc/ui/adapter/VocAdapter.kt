package com.example.sampleview.voc.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.QuickViewHolder
import com.example.sampleview.R
import com.example.sampleview.voc.ui.VocItem

/**
 * 问卷选项 RecyclerView Adapter
 *
 * @param vocItems 选项列表
 */
class VocAdapter(vocItems: List<VocItem>) : BaseQuickAdapter<VocItem, QuickViewHolder>(vocItems) {

    override fun onBindViewHolder(holder: QuickViewHolder, position: Int, item: VocItem?) {
        item ?: return
        val tvItem = holder.itemView as TextView
        tvItem.text = item.string
        tvItem.setTextColor(if (item.select) "#00B14F".toColorInt() else "#1A1A1A".toColorInt())
    }

    override fun onCreateViewHolder(context: Context, parent: ViewGroup, viewType: Int): QuickViewHolder {
        return QuickViewHolder(LayoutInflater.from(context).inflate(R.layout.item_voc, parent, false))
    }
}