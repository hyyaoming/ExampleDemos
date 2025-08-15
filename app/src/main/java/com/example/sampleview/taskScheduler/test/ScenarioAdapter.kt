package com.example.sampleview.taskScheduler.test

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScenarioAdapter(
    private val scenarios: List<TestScenario>,
    private val itemClick: (TestScenario) -> Unit
) : RecyclerView.Adapter<ScenarioAdapter.ScenarioViewHolder>() {

    inner class ScenarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: Button = itemView as Button

        init {
            itemView.setOnClickListener {
                itemClick(scenarios[absoluteAdapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScenarioViewHolder {
        val v = Button(parent.context)
        return ScenarioViewHolder(v)
    }

    override fun onBindViewHolder(holder: ScenarioViewHolder, position: Int) {
        holder.tvName.text = scenarios[position].name
    }

    override fun getItemCount(): Int = scenarios.size
}
