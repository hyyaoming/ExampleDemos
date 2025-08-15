package com.example.sampleview.taskflowengine

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

class TaskFlowEngine<I, O> {
    private var steps: MutableList<TaskStep<*, *>> = mutableListOf()
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lifecycle: Lifecycle? = null
    private var lifecycleObserver: LifecycleEventObserver? = null
    private val listeners = CopyOnWriteArrayList<TaskFlowListener<I, O>>()
    private var job: Job? = null
    private val _isStopped = AtomicBoolean(false)
    val isStopped get() = _isStopped.get()

    fun addStep(step: TaskStep<*, *>) = apply {
        this.steps.add(step)
    }

    fun steps(steps: MutableList<TaskStep<*, *>>) = apply {
        this.steps = steps
    }

    fun scope(scope: CoroutineScope) = apply {
        this.scope = scope
    }

    fun lifecycle(lifecycle: Lifecycle) = apply {
        this.lifecycle?.removeObserver(lifecycleObserver ?: return@apply)
        this.lifecycle = lifecycle
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    stop()
                    lifecycle.removeObserver(this)
                }
            }
        }
        lifecycleObserver = observer
        lifecycle.addObserver(observer)
    }

    fun addListener(listener: TaskFlowListener<I, O>) = apply {
        listeners.add(listener)
    }

    fun removeListener(listener: TaskFlowListener<I, O>) = apply {
        listeners.remove(listener)
    }

    @Suppress("UNCHECKED_CAST")
    fun start(input: I) {
        if (_isStopped.get()) {
            Log.d(TAG, "任务链已停止，无法启动")
            return
        }
        if (job?.isActive == true) {
            Log.d(TAG, "任务链已经在执行中")
            return
        }
        listeners.forEach { it.onChainStart() }
        job = scope.launch {
            var currentInput: Any? = input
            try {
                steps.forEachIndexed { index, step ->
                    if (_isStopped.get()) throw CancellationException("任务链被停止")
                    val taskStep = step as TaskStep<Any?, Any?>
                    val stepName = taskStep.stepName
                    Log.d(TAG, "执行步骤 ${index + 1}/${steps.size}: $stepName")
                    currentInput = withContext(taskStep.dispatcher) {
                        if (taskStep.timeoutMillis > 0) {
                            withTimeout(taskStep.timeoutMillis) {
                                taskStep.execute(currentInput)
                            }
                        } else {
                            taskStep.execute(currentInput)
                        }
                    }
                }
                if (_isStopped.compareAndSet(false, true)) {
                    listeners.forEach { it.onChainComplete(currentInput as O) }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "任务链执行取消: ${e.message}")
            } catch (e: Throwable) {
                if (_isStopped.compareAndSet(false, true)) {
                    val failedStepName = "执行步骤异常"
                    listeners.forEach { it.onChainFailure(failedStepName, e) }
                }
            }
        }
    }

    fun stop() {
        if (_isStopped.compareAndSet(false, true)) {
            job?.cancel()
            Log.d(TAG, "任务链已取消")
            lifecycle?.removeObserver(lifecycleObserver ?: return)
            lifecycleObserver = null
            lifecycle = null
            listeners.clear()
        }
    }

    companion object {
        private const val TAG = "TaskFlowEngine"
    }
}
