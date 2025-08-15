package com.example.sampleview.reddot

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.R
import com.example.sampleview.reddot.aggregator.RedDotAggregator
import com.example.sampleview.reddot.aggregator.RedDotAggregator.AggregationMatcher
import com.example.sampleview.reddot.core.RedDotData
import com.example.sampleview.reddot.core.RedDotKey
import com.example.sampleview.reddot.core.RedDotKeys
import com.example.sampleview.reddot.core.RedDotValue
import com.example.sampleview.reddot.datasource.RedDotDataSource
import com.example.sampleview.reddot.ui.RedDotView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RedDotTestActivity : AppCompatActivity(R.layout.activity_red_dot_test) {
    private lateinit var redDotView: RedDotView
    private lateinit var redDotNumberView: RedDotView
    private lateinit var redDotAggregator: RedDotView
    private val feedbackIds = listOf("1", "2", "3", "4", "5", "6", "7")
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        redDotView = findViewById<RedDotView>(R.id.redDotView)
        redDotNumberView = findViewById<RedDotView>(R.id.redDotNumberView)
        redDotAggregator = findViewById<RedDotView>(R.id.tvAggregatorCount)

        // 注册假的数据源
        RedDotManager.registerDataSource(RedDotKeys.Profile.root, object : RedDotDataSource<List<String>> {

            override suspend fun fetchRaw(): List<String> {
                delay(1000)
                return listOf("")
            }

            override val adapter = object : RedDotDataSource.RedDotAdapter<List<String>> {

                override suspend fun adapt(raw: List<String>): List<RedDotData> {
                    return listOf(
                        RedDotData(RedDotKeys.Profile.feedBackUnRead("1"), RedDotValue.Number(1)),
                        RedDotData(RedDotKeys.Profile.feedBackUnRead("2"), RedDotValue.Number(1)),
                        RedDotData(RedDotKeys.Profile.feedBackUnRead("3"), RedDotValue.Number(1)),
                        RedDotData(RedDotKeys.Profile.feedBackUnRead("4"), RedDotValue.Number(1)),
                        RedDotData(RedDotKeys.Profile.feedBackUnRead("5"), RedDotValue.Number(1)),
                        RedDotData(RedDotKeys.Profile.feedBackUnRead("6"), RedDotValue.Number(1)),
                        RedDotData(RedDotKeys.Profile.feedBackUnRead("7"), RedDotValue.Number(1)),
                        RedDotData(RedDotKeys.Profile.updateApp.build(), RedDotValue.Dot),
                        RedDotData(RedDotKeys.Profile.chatMessage.build(), RedDotValue.Number(12))
                    )
                }

                override suspend fun reportRead(
                    keys: List<RedDotKey>,
                    extraInfo: Map<String, Any>?,
                ): List<RedDotKey> {
                    return keys
                }
            }
        })

        observeRedDotStates()

        findViewById<Button>(R.id.btnReadRedDot).setOnClickListener {
            lifecycleScope.launch {
                RedDotManager.submitReadStatus(RedDotKeys.Profile.updateApp.build())
            }
        }
        findViewById<Button>(R.id.btnReadNumberRedDot).setOnClickListener {
            lifecycleScope.launch {
                RedDotManager.submitReadStatus(RedDotKeys.Profile.chatMessage.build())
            }
        }
        findViewById<Button>(R.id.btnReadCount).setOnClickListener {
            lifecycleScope.launch {
                if (currentIndex < feedbackIds.count()) {
                    RedDotManager.submitReadStatus(RedDotKeys.Profile.feedBackUnRead(feedbackIds[currentIndex]))
                    currentIndex++
                }
            }
        }
    }

    private fun observeRedDotStates() {
        RedDotManager.registerAggregator(object : RedDotAggregator<RedDotValue> {
            override val matcher = object : AggregationMatcher {
                override fun matches(path: String): Boolean {
                    return path.startsWith(RedDotKeys.Profile.feedbackUnReadPrefix)
                }
            }

            override suspend fun aggregate(redDots: Map<RedDotKey, RedDotData>): RedDotValue {
                val total = redDots.values
                    .asSequence()
                    .mapNotNull { it.redDotValue as? RedDotValue.Number }
                    .sumOf { it.count }
                return if (total > 0) RedDotValue.Number(total) else RedDotValue.Empty
            }
        })

        redDotView.bindRedDotKey(this, RedDotKeys.Profile.updateApp.build())
        redDotNumberView.bindRedDotKey(this, RedDotKeys.Profile.chatMessage.build())
        redDotAggregator.bindRedDotPath(this, RedDotKeys.Profile.feedbackUnReadPrefix)


        // 初始模拟加载
        lifecycleScope.launch {
            RedDotManager.refreshAllDataSources()
        }
    }
}
