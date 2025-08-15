package com.example.sampleview.taskScheduler.test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sampleview.R
import com.example.sampleview.taskScheduler.api.Task
import com.example.sampleview.taskScheduler.api.TaskContext
import com.example.sampleview.taskScheduler.core.TaskScheduler
import com.example.sampleview.taskScheduler.impl.AbstractTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TaskActivity : AppCompatActivity() {

    private lateinit var rvScenarios: RecyclerView
    private lateinit var rvLogs: RecyclerView
    private lateinit var scenarioAdapter: ScenarioAdapter
    private lateinit var logAdapter: LogAdapter

    // 你的 TaskScheduler 实例
    private var scheduler: TaskScheduler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        rvScenarios = findViewById(R.id.rvScenarios)
        rvLogs = findViewById(R.id.rvLogs)

        logAdapter = LogAdapter()
        rvLogs.apply {
            layoutManager = LinearLayoutManager(this@TaskActivity)
            adapter = logAdapter
        }

        val scenarios = listOf(TestScenario("正常执行") {
            runScenarioNormal()
        }, TestScenario("失败场景") {
            runScenarioFailure()
        }, TestScenario("取消场景") {
            runScenarioCancel()
        }, TestScenario("取消调度") {
            scheduler?.cancel()
            log("已请求取消调度")
        })

        scenarioAdapter = ScenarioAdapter(scenarios) { scenario ->
            logAdapter.clear()
            log("开始执行场景: ${scenario.name}")
            lifecycleScope.launch {
                try {
                    scenario.run()
                } catch (e: Exception) {
                    log("场景异常: ${e.message}")
                }
            }
        }

        rvScenarios.apply {
            layoutManager = LinearLayoutManager(this@TaskActivity)
            adapter = scenarioAdapter
        }
    }

    private fun log(msg: String) {
        runOnUiThread {
            logAdapter.addLog("${System.currentTimeMillis().toTimeString()} $msg")
        }
    }

    // 扩展函数，格式化时间戳
    private fun Long.toTimeString(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(this))
    }

    /**
     * 下面写你的几个测试场景的模拟实现
     */
    private suspend fun runScenarioNormal() {
        val taskA = object : AbstractTask<String>() {
            override val dispatcher: CoroutineDispatcher = Dispatchers.Main
            override val id: String = "A"
            override val name = "任务A"
            override suspend fun executeTask(context: TaskContext): String? {
                log("任务A 执行中..." + Thread.currentThread().name)
                delay(3000)
                log("任务A 完成")
                return "结果A"
            }
        }
        val taskB = object : AbstractTask<String>() {
            override val dispatcher: CoroutineDispatcher
                get() = Dispatchers.IO
            override val id: String = "B"
            override fun dependsOn(): List<Class<out Task<*>>> {
                return listOf(taskA::class.java)
            }
            override val name = "任务B"
            override suspend fun executeTask(taskContext: TaskContext): String? {
                log("任务B 执行中..." + Thread.currentThread().name)
                delay(2000)
                log("任务B 完成")
                return "结果B"
            }
        }
        val taskC = object : AbstractTask<String>() {
            override val dispatcher: CoroutineDispatcher = Dispatchers.Main
            override val id: String = "C"
            override val name = "任务C"
            override fun dependsOn(): List<Class<out Task<*>>> {
                return listOf(taskB::class.java,taskA::class.java)
            }
            override suspend fun executeTask(taskContext: TaskContext): String? {
                log("任务C 执行中..." + Thread.currentThread().name)
                delay(600)
                log("任务C 完成")
                return "结果C"
            }
        }
        val scheduler = TaskScheduler.create(listOf(taskA, taskB, taskC))
        this.scheduler = scheduler
        val result = scheduler.executeAll()
        log("调度完成，耗时：${result.totalTimeMillis}ms，失败任务数：${result.taskExceptions.size}")
    }

    private suspend fun runScenarioFailure() {
        val taskA = object : AbstractTask<String>() {
            override val id: String = "A"
            override val name = "任务A"
            override suspend fun executeTask(taskContext: TaskContext): String? {
                log("任务A 执行中...")
                kotlinx.coroutines.delay(500)
                log("任务A 完成")
                return "结果A"
            }
        }
        val taskFail = object : AbstractTask<String>() {
            override val id: String = "Fail"
            override val name = "任务失败"
            override suspend fun executeTask(taskContext: TaskContext): String? {
                log("任务失败 执行中...")
                delay(300)
                throw RuntimeException("故意失败")
            }
        }
        val scheduler = TaskScheduler.create(listOf(taskA, taskFail))
        this.scheduler = scheduler
        val result = scheduler.executeAll()
        log("调度完成，耗时：${result.totalTimeMillis}ms，失败任务数：${result.taskExceptions.size}")
        if (result.taskExceptions.isNotEmpty()) {
            for ((task, ex) in result.taskExceptions) {
                log("任务失败: ${task.name} 异常: ${ex.message}")
            }
        }
    }

    private suspend fun runScenarioCancel() {
        val longTask = object : AbstractTask<String>() {
            override val dispatcher: CoroutineDispatcher = Dispatchers.Main
            override val id: String = "longTask"
            override val name = "长任务"
            override suspend fun executeTask(taskContext: TaskContext): String? {
                log("长任务开始执行...")
                repeat(10) {
                    delay(500)
                    log("长任务进行中 $it")
                    ensureActive()
                }
                log("长任务完成")
                return "长任务结果"
            }
        }
        val scheduler = TaskScheduler.create(listOf(longTask))

        val job = lifecycleScope.launch {
            scheduler.executeAll()
            log("调度完成")
        }

        lifecycleScope.launch {
            delay(2000)  // 等任务运行一会儿
            scheduler.cancel()  // 触发取消
            log("取消请求发送")
        }

        job.join() // 等待调度协程结束

    }

}
