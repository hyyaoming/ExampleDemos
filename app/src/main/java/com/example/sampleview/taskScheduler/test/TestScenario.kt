package com.example.sampleview.taskScheduler.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// 测试场景数据类
data class TestScenario(val name: String, val run: suspend () -> Unit)

// 日志适配器
class LogAdapter : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {
    private val logs = mutableListOf<String>()

    inner class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLog: TextView = itemView.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return LogViewHolder(v)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.tvLog.text = logs[position]
    }

    override fun getItemCount(): Int = logs.size

    fun addLog(log: String) {
        logs.add(0, log) // 新日志加顶部
        notifyItemInserted(0)
    }

    fun clear() {
        logs.clear()
        notifyDataSetChanged()
    }
}
