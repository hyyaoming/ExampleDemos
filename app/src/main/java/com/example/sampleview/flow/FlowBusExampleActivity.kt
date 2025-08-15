package com.example.sampleview.flow

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class FlowBusTestActivity : AppCompatActivity() {

    private val TAG = "FlowBusTest"
    private var job: Job? = null
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_flow)

        tvResult = findViewById(R.id.tv_result)

        findViewById<Button>(R.id.btn_tc01).setOnClickListener { testTC01() }
        findViewById<Button>(R.id.btn_tc02).setOnClickListener { testTC02() }
        findViewById<Button>(R.id.btn_tc03).setOnClickListener { testTC03() }
        findViewById<Button>(R.id.btn_tc04).setOnClickListener { testTC04() }
        findViewById<Button>(R.id.btn_tc05).setOnClickListener { testTC05() }
        findViewById<Button>(R.id.btn_tc06).setOnClickListener { testTC06() }
        findViewById<Button>(R.id.btn_tc07).setOnClickListener { testTC07() }
        findViewById<Button>(R.id.btn_tc08).setOnClickListener { testTC08() }
        findViewById<Button>(R.id.btn_tc09).setOnClickListener { testTC09() }
        initObserveEvent()
    }

    private fun initObserveEvent() {
        FlowBus.observeEvent<String>(this, "TC01") {
            appendResult("TC01 订阅者收到: $it")
        }

        FlowBus.observeEvent<String>(this, "TC02") {
            appendResult("TC02 订阅者收到: $it")
        }

        // 用一个 Job 模拟订阅者生命周期
        job = FlowBus.observeEvent<String>(this, "TC06") {
            appendResult("TC06 订阅者收到: $it")
        }

        FlowBus.observeEvent<String>(this, "TC07") {
            appendResult("TC07 订阅者收到: $it")
        }

        FlowBus.observeEvent<String>(this, "TC08") {
            appendResult("TC08 订阅者1收到: $it")
        }

        FlowBus.observeEvent<String>(this, "TC08") {
            appendResult("TC08 订阅者2收到: $it")
        }

        FlowBus.observeEvent<User>(this, "TC09") {
            appendResult("TC09 收到 User: ${it.name}, ${it.age}")
        }
    }

    private fun appendResult(msg: String) {
        runOnUiThread {
            tvResult.append("\n$msg")
        }
        Log.d(TAG, msg)
    }

    /** TC01 普通事件发送和接收
     * 订阅 -> 发送，订阅者收到事件
     */
    private fun testTC01() {
        val key = "TC01"
        tvResult.text = "TC01 测试开始..."
        FlowBus.post(key, "Hello")
    }

    /** TC02 普通事件订阅在发送事件前订阅 */
    private fun testTC02() {
        val key = "TC02"
        tvResult.text = "TC02 测试开始..."
        FlowBus.post(key, "Event before send")
    }

    /** TC03 普通事件订阅在发送事件后订阅，不会收到之前事件 */
    private fun testTC03() {
        val key = "TC03"
        tvResult.text = "TC03 测试开始..."
        FlowBus.post(key, "Sent before subscribe")
        FlowBus.observeEvent<String>(this, key) {
            appendResult("TC03 订阅者收到: $it (不应收到此条)")
        }
    }

    /** TC04 粘性事件发送和订阅，订阅后立即收到最新值 */
    private fun testTC04() {
        val key = "TC04"
        tvResult.text = "TC04 测试开始..."
        FlowBus.postSticky(key, 100)
        FlowBus.observeStickyEvent<Int>(this, "TC04") {
            appendResult("TC04 订阅者收到粘性事件: $it")
        }
    }

    /** TC05 粘性事件更新，订阅后收到最新值 */
    private fun testTC05() {
        val key = "TC05"
        tvResult.text = "TC05 测试开始..."
        FlowBus.postSticky(key, 100)
        FlowBus.postSticky(key, 200)
        FlowBus.observeStickyEvent<Int>(this, "TC05") {
            appendResult("TC05 订阅者收到最新粘性事件: $it")
        }
    }

    /** TC08 自动清理事件订阅者注销后自动清理资源 */
    private fun testTC06() {
        val key = "TC06"
        tvResult.text = "TC06 测试开始..."

        FlowBus.post(key, "Event before cancel")
        job?.cancel()  // 取消订阅
        appendResult("TC06 订阅已取消")
        FlowBus.post(key, "Event after cancel (不应收到)")
    }

    /** TC09 生命周期感知订阅，生命周期结束自动取消 */
    private fun testTC07() {
        val key = "TC07"
        tvResult.text = "TC07 测试开始..."
        FlowBus.post(key, "Event before finish")
        // 模拟生命周期结束 by 直接取消当前协程
        lifecycleScope.cancel()
        appendResult("TC07 协程取消，停止接收")
        FlowBus.post(key, "Event after cancel (不应收到)")
    }

    /** TC10 多订阅者订阅同一事件，两个订阅者都收到 */
    private fun testTC08() {
        val key = "TC08"
        tvResult.text = "TC08 测试开始..."
        FlowBus.post(key, "Multi-subscriber event")
    }

    /** TC11 事件类型泛型支持，发送复杂类型事件 */
    data class User(val name: String, val age: Int)

    private fun testTC09() {
        val key = "TC09"
        tvResult.text = "TC09 测试开始..."
        FlowBus.post(key, User("Alice", 30))
    }
}
