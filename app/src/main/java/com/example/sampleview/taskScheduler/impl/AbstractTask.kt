package com.example.sampleview.taskScheduler.impl


import TaskException
import com.example.sampleview.taskScheduler.api.RetryStrategy
import com.example.sampleview.taskScheduler.api.Task
import com.example.sampleview.taskScheduler.api.TaskContext
import com.example.sampleview.taskScheduler.core.TaskStateMachine
import com.example.sampleview.taskScheduler.model.TaskStatus
import com.example.sampleview.taskScheduler.util.TaskSchedulerLog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext

/**
 * 抽象基类，表示一个具备依赖关系、优先级、重试机制的异步任务。
 *
 * 任务执行过程包含依赖检查、延迟启动、重试控制、状态管理和耗时统计。
 *
 * @param R 任务执行结果类型
 *
 * @property id 任务唯一标识，用于区分不同任务
 * @property name 任务名称，方便日志和调试时识别
 * @property priority 任务优先级，数值越大优先级越高，决定同层任务执行顺序，默认 0
 * @property dispatcher 任务执行的协程调度器，默认使用 [Dispatchers.IO]
 * @property retryStrategy 重试策略接口，定义失败后是否重试及重试延迟，默认使用 [DefaultRetryStrategy]
 * @property delayMillis 任务执行前的延迟时间（毫秒），默认 0 表示不延迟
 * @property dependencies 当前任务的依赖任务列表，所有依赖任务必须成功后才会执行当前任务
 *
 * @property status 任务当前状态，标记任务的执行阶段，初始为 [TaskStatus.PENDING]
 * @property executionTimeMillis 任务执行耗时，单位毫秒，包含重试等所有尝试时间，任务结束后更新
 *
 * @see RetryStrategy
 * @see TaskStateMachine
 * @see TaskStatus
 * @see Dispatchers.IO
 * @see withContext
 * @see delay
 */
abstract class AbstractTask<R> : Task<R> {

    /** 任务唯一 ID，用于标识不同任务 */
    abstract override val id: String

    /** 任务名称，便于日志输出和调试 */
    abstract override val name: String

    /** 任务优先级，数值越大优先执行，默认为 0 */
    override val priority: Int = 0

    /** 失败重试策略，默认使用 [DefaultRetryStrategy]，可禁用重试通过设置为 null */
    override val retryStrategy: RetryStrategy? = DefaultRetryStrategy()

    /** 任务执行所使用的协程调度器，默认使用 IO 线程 */
    override val dispatcher: CoroutineDispatcher = Dispatchers.Default

    /** 任务执行超时时间，毫秒，0 表示不超时 */
    override val timeoutMillis: Long = 0

    /** 任务执行前的延迟时间，单位毫秒，默认为 0 表示无延迟 */
    override val delayMillis: Long = 0

    /** 公开的只读依赖任务列表 */
    override val dependencies: List<Task<*>> get() = _dependencies

    /** 任务执行结果 */
    override val result: R? get() = _result

    /** 任务总耗时（毫秒），包含所有重试耗时，任务结束后自动计算 */
    override var executionTimeMillis: Long = 0

    /** 当前任务状态，从状态机中获取 */
    override val status: TaskStatus get() = stateMachine.currentState

    /** 内部维护的依赖任务列表 */
    private val _dependencies = mutableListOf<Task<*>>()

    /** 任务取消标志，保证取消只执行一次 */
    private val isCanceled = AtomicBoolean(false)

    /** 任务执行结果 */
    private var _result: R? = null

    /** 协程信号，用于通知依赖任务当前任务已完成（成功/失败/取消） */
    private val completionSignal = CompletableDeferred<Unit>()

    /** 状态机：用于管理任务状态，状态变更时记录日志 */
    private val stateMachine = TaskStateMachine(onStateChanged = { old, new ->
        TaskSchedulerLog.d("[$name] 状态变化：$old → $new")
    })

    /**
     * 主动取消任务。
     *
     * 取消任务并取消所有协程，状态变为 CANCELED，取消回调仅执行一次。
     */
    override fun cancel() {
        if (isCanceled.compareAndSet(false, true)) {
            try {
                stateMachine.transitionTo(TaskStatus.CANCELED)
            } catch (e: IllegalStateException) {
                TaskSchedulerLog.e("取消时状态转换失败", e)
            }
            safeOnCanceled()
        }
    }

    /**
     * 执行当前任务。
     *
     * 支持等待依赖完成，执行超时，取消检测，重试机制。
     *
     * @param  taskContext 用于任务之间的数据传递
     * @return 成功时返回执行结果，类型为 R
     * @throws CancellationException 若任务被取消或依赖任务失败
     * @throws Throwable 其他执行中未被处理的异常
     */
    override suspend fun execute(taskContext: TaskContext): R? {
        ensureActive()
        TaskSchedulerLog.d("准备执行任务: $name")
        awaitDependencies()
        if (delayMillis > 0) {
            TaskSchedulerLog.d("任务[$name] 延迟 $delayMillis ms 执行")
            delay(delayMillis)
        }
        checkIfCanceled()
        stateMachine.transitionTo(TaskStatus.RUNNING)
        safeOnStart()
        TaskSchedulerLog.d("任务[$name] 开始执行")
        val startTime = System.currentTimeMillis()
        return try {
            val result = if (timeoutMillis > 0) {
                withTimeout(timeoutMillis) {
                    runWithRetry(taskContext)
                }
            } else {
                runWithRetry(taskContext)
            }
            handleSuccess(result, startTime)
            result
        } catch (ce: CancellationException) {
            handleCancellation(ce, startTime)
            throw ce
        } catch (e: Throwable) {
            handleFailure(e, startTime)
            throw e
        }
    }

    /**
     * 添加依赖任务，支持传入多个任务。
     *
     * @param tasks 依赖的任务列表
     * @return 当前任务实例
     */
    override fun addDependsOn(vararg tasks: Task<*>) = apply {
        tasks.forEach { task ->
            if (!_dependencies.contains(task)) {
                _dependencies.add(task)
            }
        }
    }

    /**
     * 调用该方法的协程会被挂起，直到任务完成信号被触发
     */
    override suspend fun awaitCompletion() {
        completionSignal.await()
    }

    /**
     * 等待依赖任务全部成功完成。
     *
     * 若任意依赖任务失败或取消，抛出 CancellationException。
     */
    private suspend fun awaitDependencies() {
        dependencies.forEach { dep ->
            dep.awaitCompletion()
            if (dep.status != TaskStatus.SUCCESS) {
                throw CancellationException("依赖任务 ${dep.name} 状态为 ${dep.status}，取消执行")
            }
            checkIfCanceled()
        }
    }

    /**
     * 检查当前任务是否已被取消，若是则抛出异常。
     *
     * @throws CancellationException 如果任务已被取消
     */
    private fun checkIfCanceled() {
        if (isCanceled.get()) {
            stateMachine.transitionTo(TaskStatus.CANCELED)
            safeOnCanceled()
            throw CancellationException("任务[$name] 已被取消")
        }
    }

    /**
     * 检查任务和协程是否已取消，确保任务处于激活状态。
     * 若被取消则抛出 CancellationException。
     */
    protected suspend fun ensureActive() {
        checkIfCanceled()
        coroutineContext.ensureActive()
    }

    /**
     * 执行任务主体并根据重试策略在失败后进行重试。
     *
     * 支持取消检测与最大重试次数控制。
     * @param  taskContext 用于任务之间的数据传递
     * @return 成功执行结果
     * @throws Throwable 若执行失败且不再重试
     */
    private suspend fun runWithRetry(taskContext: TaskContext): R? {
        var lastException: Throwable? = null
        val maxAttempts = (retryStrategy?.maxRetryCount ?: 0) + 1
        for (attempt in 1..maxAttempts) {
            checkIfCanceled()
            try {
                return withContext(coroutineContext + dispatcher) { executeTask(taskContext) }
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                lastException = e
                TaskSchedulerLog.e("任务[$name] 第 $attempt 次执行失败", throwable = e)
                if (!canRetry(e)) throw e
                delayBeforeRetry(attempt, e)
            }
        }
        throw TaskException("任务[$name] 执行失败", lastException)
    }

    /**
     * 处理任务执行成功场景。
     * 设置状态为 SUCCESS，记录耗时并回调 [onSuccess]。
     */
    private fun handleSuccess(result: R?, startTime: Long) {
        _result = result
        try {
            stateMachine.transitionTo(TaskStatus.SUCCESS)
            updateExecutionTime(startTime)
            safeOnSuccess(result)
            TaskSchedulerLog.d("任务[$name] 执行成功，耗时=${executionTimeMillis}ms")
        } catch (t: Throwable) {
            TaskSchedulerLog.e("任务[$name] 处理成功回调异常", t)
        } finally {
            markComplete()
        }
    }

    /**
     * 处理任务执行失败场景。
     * 设置状态为 FAILED，记录耗时并回调 [onFailure]。
     */
    private fun handleFailure(e: Throwable, startTime: Long) {
        try {
            stateMachine.transitionTo(TaskStatus.FAILED)
            updateExecutionTime(startTime)
            safeOnFailure(e)
            TaskSchedulerLog.e("任务[$name] 执行失败，耗时=${executionTimeMillis}ms", e)
        } catch (t: Throwable) {
            TaskSchedulerLog.e("任务[$name] 处理失败回调异常", t)
        } finally {
            markComplete()
        }
    }

    /**
     * 处理任务被取消场景。
     * 设置状态为 CANCELED，记录耗时并回调 [onCanceled]。
     */
    private fun handleCancellation(e: CancellationException, startTime: Long) {
        try {
            stateMachine.transitionTo(TaskStatus.CANCELED)
            updateExecutionTime(startTime)
            safeOnCanceled()
            TaskSchedulerLog.e("任务[$name] 被取消，耗时=${executionTimeMillis}ms", e)
        } catch (t: Throwable) {
            TaskSchedulerLog.e("任务[$name] 处理取消回调异常", t)
        } finally {
            markComplete()
        }
    }

    /**
     * 计算总的耗时时间
     */
    private fun updateExecutionTime(startTime: Long) {
        executionTimeMillis = System.currentTimeMillis() - startTime
    }

    /**
     * 标记任务已完成，唤醒所有等待任务依赖的协程
     */
    private fun markComplete() {
        if (!completionSignal.isCompleted) {
            completionSignal.complete(Unit)
        }
    }

    /**
     * 判断当前异常情况下是否允许重试。
     *
     * 委托给 [retryStrategy] 实现，若重试策略为 null，则不允许重试。
     */
    private fun canRetry(e: Throwable) = retryStrategy?.shouldRetry(e) == true

    /**
     * 执行重试逻辑。
     *
     * 设置状态为 RETRYING，调用重试回调，等待重试延迟后返回。
     */
    private suspend fun delayBeforeRetry(attempt: Int, e: Throwable) {
        stateMachine.transitionTo(TaskStatus.RETRYING)
        safeOnRetry(attempt, e)
        val delayTime = retryStrategy?.getRetryDelay(attempt) ?: RetryStrategy.Companion.BASE_DELAY_MS
        TaskSchedulerLog.d("任务[$name] 第 $attempt 次重试，延迟 ${delayTime}ms")
        delay(delayTime)
    }

    /**
     * 具体任务的执行逻辑，由子类实现
     *
     * @param  taskContext 用于任务之间的数据传递
     */
    protected abstract suspend fun executeTask(taskContext: TaskContext): R?

    /** 安全调用 onStart，防止回调异常影响任务执行 */
    private fun safeOnStart() = try {
        onStart()
    } catch (t: Throwable) {
        TaskSchedulerLog.e("onStart 回调异常", t)
    }

    /** 安全调用 onSuccess，防止回调异常影响任务执行 */
    private fun safeOnSuccess(result: R?) = try {
        onSuccess(result)
    } catch (t: Throwable) {
        TaskSchedulerLog.e("onSuccess 回调异常", t)
    }

    /** 安全调用 onFailure，防止回调异常影响任务执行 */
    private fun safeOnFailure(e: Throwable) = try {
        onFailure(e)
    } catch (t: Throwable) {
        TaskSchedulerLog.e("onFailure 回调异常", t)
    }

    /** 安全调用 onRetry，防止回调异常影响任务执行 */
    private fun safeOnRetry(attempt: Int, e: Throwable) = try {
        onRetry(attempt, e)
    } catch (t: Throwable) {
        TaskSchedulerLog.e("onRetry 回调异常", t)
    }

    /** 安全调用 onCanceled，防止回调异常影响任务执行 */
    private fun safeOnCanceled() = try {
        onCanceled()
    } catch (t: Throwable) {
        TaskSchedulerLog.e("onCanceled 回调异常", t)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Task<*>) return false
        return id == other.id && name == other.name
    }
}
