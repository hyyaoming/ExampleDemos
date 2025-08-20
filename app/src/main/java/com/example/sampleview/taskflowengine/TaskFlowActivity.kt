package com.example.sampleview.taskflowengine

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sampleview.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TaskFlowActivity : AppCompatActivity(R.layout.activity_task_flow) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.btnStartTaskFlow).setOnClickListener {
            lifecycleScope.launch {
                val engine = taskFlowEngine {
                    step<String, String>("Step1") {
                        delay(500)
                        "$it world"
                    }
                    step<String, Int>("Step2", timeoutMillis = 1000) {
                        it.length
                    }
                    step(object : TaskStep<Int, Boolean> {
                        override val stepName: String get() = "CustomStep"
                        override suspend fun execute(input: Int): Boolean {
                            delay(1000)
                            return input == 5
                        }
                    })
                }
                engine.startFlow("Hello").collect { result ->
                    when (result) {
                        is StepResult.StepStarted -> {
                            println("开始: ${result.stepName}, 输入: ${result.input}")
                        }
                        is StepResult.StepCompleted -> {
                            println("完成: ${result.stepName}, 输出: ${result.output}, 耗时: ${result.durationMillis} ms")
                        }
                        is StepResult.ChainCompleted -> {
                            println("任务链完成，最终结果: ${result.result}, 总耗时: ${result.totalDurationMillis} ms")
                        }
                        is StepResult.ChainCancelled -> {
                            println("任务链取消: ${result.cause}")
                        }
                        is StepResult.ChainFailed -> {
                            println("任务链失败: ${result.throwable}")
                        }
                    }
                }
            }
        }
    }

}