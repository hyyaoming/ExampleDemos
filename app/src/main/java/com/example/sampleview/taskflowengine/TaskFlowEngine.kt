package com.example.sampleview.taskflowengine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

/**
 * 封装任务步骤执行结果，用于在任务链执行过程中通知状态。
 *
 * 通过 [TaskFlowEngine] 执行任务链时，会依次发出不同类型的 [StepResult]：
 * - [StepStarted]：某个步骤开始执行
 * - [StepCompleted]：某个步骤执行完成
 * - [ChainCompleted]：任务链全部完成
 * - [ChainCancelled]：任务链被取消
 * - [ChainFailed]：任务链执行失败
 */
sealed class StepResult {

    /**
     * 某个步骤开始执行的事件。
     *
     * @property stepName 步骤名称，用于日志或调试
     * @property input 步骤的输入数据
     */
    data class StepStarted(val stepName: String, val input: Any?) : StepResult()

    /**
     * 某个步骤执行完成的事件。
     *
     * @property stepName 步骤名称，用于日志或调试
     * @property output 步骤输出数据
     * @property durationMillis 步骤执行耗时（毫秒）
     */
    data class StepCompleted(val stepName: String, val output: Any?, val durationMillis: Long) : StepResult()

    /**
     * 整个任务链执行完成的事件。
     *
     * @property result 任务链最终输出结果
     * @property totalDurationMillis 任务链总耗时（毫秒）
     */
    data class ChainCompleted(val result: Any?, val totalDurationMillis: Long) : StepResult()

    /**
     * 任务链被取消的事件。
     *
     * @property cause 取消原因，通常是 [CancellationException]
     */
    data class ChainCancelled(val cause: CancellationException) : StepResult()

    /**
     * 任务链执行失败的事件。
     *
     * @property throwable 导致失败的异常
     */
    data class ChainFailed(val throwable: Throwable) : StepResult()
}

/**
 * 泛型任务步骤接口，用于定义任务链中每个独立步骤的执行逻辑。
 *
 * 任务链通过依次执行一系列 [TaskStep]，每个步骤可以接收上一步的输出作为输入，
 * 并返回自己的输出供下一步使用。通过协程调度器和超时控制，可以灵活控制步骤执行环境。
 *
 * @param I 输入类型，表示该步骤接收的数据类型
 * @param O 输出类型，表示该步骤返回的数据类型
 */
interface TaskStep<I, O> {

    /**
     * 步骤名称，用于日志记录、调试和追踪。
     *
     * 每个步骤应有唯一且易识别的名称，便于在任务链执行过程中跟踪具体步骤状态。
     */
    val stepName: String

    /**
     * 执行协程调度器，控制步骤逻辑在指定线程或线程池中运行。
     *
     * 默认使用 [Dispatchers.Default]，可根据步骤的 I/O 或 CPU 密集型特性选择其他调度器：
     * - [Dispatchers.IO]：适合网络请求、文件读写等 I/O 密集操作
     * - [Dispatchers.Default]：适合 CPU 密集型计算
     * - [Dispatchers.Main]：适合更新 UI（仅 Android/Kotlin UI 场景）
     */
    val dispatcher: CoroutineDispatcher
        get() = Dispatchers.Default

    /**
     * 步骤执行超时时间，单位毫秒。
     *
     * - 值为 0 表示不限制，即步骤可无限期运行
     * - 大于 0 的值表示步骤必须在指定时间内完成，否则将抛出 [kotlinx.coroutines.TimeoutCancellationException]
     */
    val timeoutMillis: Long
        get() = 0L

    /**
     * 执行步骤逻辑方法。
     *
     * 调用该方法时，任务链会传入上一步的输出（或初始输入），返回该步骤的输出结果。
     * 可在方法内部执行任意 suspend 操作，例如网络请求、数据库操作、计算任务等。
     *
     * @param input 步骤输入数据，类型为 [I]，可能来自上一步的输出或任务链初始输入
     * @return 步骤输出数据，类型为 [O]，将传递给下一步骤
     *
     * @throws Exception 步骤内部可能抛出的异常，将导致任务链触发 [StepResult.ChainFailed]
     */
    suspend fun execute(input: I): O
}

/**
 * Lambda 方式实现 [TaskStep]，用于快速创建任务步骤。
 *
 * 通过此类，你可以无需手动实现接口，直接传入步骤逻辑 lambda 即可。
 * 支持自定义步骤名称、调度器和超时控制。
 *
 * 适用于快速构建任务链或简单步骤逻辑，无需额外类文件。
 *
 * @param I 输入类型，表示该步骤接收的数据类型
 * @param O 输出类型，表示该步骤返回的数据类型
 *
 * @property stepName 步骤名称，用于日志记录、调试和追踪
 * @property dispatcher 执行协程调度器，默认 [Dispatchers.Default]，可根据任务类型选择合适的调度器：
 * - [Dispatchers.IO]：适合 I/O 密集操作
 * - [Dispatchers.Default]：适合 CPU 密集计算
 * - [Dispatchers.Main]：适合 UI 更新（Android 场景）
 * @property timeoutMillis 步骤执行超时时间，单位毫秒，0 表示不限制
 * @property block 步骤逻辑 lambda，接收输入 [I]，返回输出 [O]，可执行 suspend 操作
 */
class TaskStepImpl<I, O>(
    override val stepName: String,
    override val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    override val timeoutMillis: Long = 0,
    private val block: suspend (I) -> O,
) : TaskStep<I, O> {

    /**
     * 执行步骤逻辑。
     *
     * @param input 步骤输入数据，类型为 [I]
     * @return 步骤输出数据，类型为 [O]
     */
    override suspend fun execute(input: I): O = block(input)
}

/**
 * 任务流引擎，用于按顺序执行一系列 [TaskStep]。
 *
 * 任务链按添加顺序执行每个步骤，每个步骤的输出会作为下一步骤的输入。
 * 支持步骤执行超时、调度器切换、任务取消以及异常捕获。
 * 执行过程通过 [Flow] 发出不同的 [StepResult]，可实时监听每一步执行状态和耗时。
 *
 * @constructor 内部构造，仅通过 [TaskFlowBuilder] 创建
 * @param steps 步骤列表，[TaskStep] 的集合，按添加顺序执行
 *
 * 使用示例：
 * ```
 * val engine = taskFlowEngine {
 *     step("Step1") { input: String -> "$input world" }       // 输出 String
 *     step("Step2") { input: String -> input.length }         // 输出 Int
 * }
 *
 * lifecycleScope.launch {
 *     engine.startFlow("Hello").collect { result ->
 *         when (result) {
 *             is StepResult.StepStarted ->
 *                 println("开始步骤: ${result.stepName}, 输入: ${result.input}")
 *             is StepResult.StepCompleted ->
 *                 println("完成步骤: ${result.stepName}, 输出: ${result.output}, 耗时: ${result.durationMillis}ms")
 *             is StepResult.ChainCompleted ->
 *                 println("任务链完成，最终结果: ${result.result}, 总耗时: ${result.totalDurationMillis}ms")
 *             is StepResult.ChainCancelled ->
 *                 println("任务链被取消: ${result.cause}")
 *             is StepResult.ChainFailed ->
 *                 println("任务链执行失败: ${result.throwable}")
 *         }
 *     }
 * }
 * ```
 */
class TaskFlowEngine internal constructor(
    private val steps: List<TaskStep<*, *>>,
) {

    private val _isStopped = AtomicBoolean(false)

    /**
     * 当前任务链是否已停止执行。
     *
     * 当值为 true 时，任务链不会执行后续步骤，且任何调用 [startFlow] 的新流程将立即结束。
     */
    val isStopped get() = _isStopped.get()

    /**
     * 启动任务链执行。
     *
     * 按步骤顺序执行任务链，每个步骤的执行状态会通过 [Flow] 发出。
     * 每个步骤完成时，会返回该步骤耗时；任务链完成时，会返回总耗时。
     *
     * @param input 任务链初始输入，可作为第一个步骤的输入
     * @return [Flow]，[StepResult] 类型，可监听每一步执行状态、耗时和异常
     *
     * @throws CancellationException 如果任务链被取消
     * @throws Throwable 如果步骤执行过程中抛出异常，将通过 [StepResult.ChainFailed] 通知
     */
    @Suppress("UNCHECKED_CAST")
    fun startFlow(input: Any?): Flow<StepResult> = flow {
        if (_isStopped.get()) return@flow
        var current: Any? = input

        val chainStartTime = System.currentTimeMillis()
        try {
            for (step in steps) {
                if (_isStopped.get()) throw CancellationException("任务链被停止")

                // 发出步骤开始事件
                emit(StepResult.StepStarted(step.stepName, current))

                // 记录步骤开始时间
                val stepStartTime = System.currentTimeMillis()

                // 切换到指定 Dispatcher 执行步骤
                current = withContext(step.dispatcher) {
                    val output = if (step.timeoutMillis > 0) {
                        withTimeout(step.timeoutMillis) {
                            (step as TaskStep<Any?, Any?>).execute(current)
                        }
                    } else {
                        (step as TaskStep<Any?, Any?>).execute(current)
                    }
                    output
                }

                // 计算步骤耗时并发出完成事件
                val duration = System.currentTimeMillis() - stepStartTime
                emit(StepResult.StepCompleted(step.stepName, current, duration))
            }

            // 所有步骤完成，计算总耗时并发出链完成事件
            val totalDuration = System.currentTimeMillis() - chainStartTime
            emit(StepResult.ChainCompleted(current, totalDuration))
        } catch (e: CancellationException) {
            emit(StepResult.ChainCancelled(e))
        } catch (e: Throwable) {
            emit(StepResult.ChainFailed(e))
        } finally {
            _isStopped.set(true)
        }
    }

    /**
     * 停止任务链执行。
     *
     * 调用后，任务链在下一个步骤开始前会被取消。
     * 任务链已执行的步骤不会回滚，任务链被取消后会发出 [StepResult.ChainCancelled]。
     */
    fun stop() {
        _isStopped.set(true)
    }
}

/**
 * TaskFlow 构建器，用于通过 DSL 添加步骤并生成 [TaskFlowEngine]
 */
class TaskFlowBuilder(private val steps: MutableList<TaskStep<*, *>> = mutableListOf()) {

    /**
     * 添加 Lambda 步骤
     *
     * @param In 步骤输入类型
     * @param Out 步骤输出类型
     * @param name 步骤名称
     * @param dispatcher 执行调度器
     * @param timeoutMillis 超时时间
     * @param block 步骤执行逻辑
     * @return 返回自身，支持链式调用
     */
    fun <In, Out> step(
        name: String,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        timeoutMillis: Long = 0,
        block: suspend (In) -> Out,
    ): TaskFlowBuilder {
        steps.add(TaskStepImpl(name, dispatcher, timeoutMillis, block))
        return this
    }

    /**
     * 添加自定义 [TaskStep] 步骤
     *
     * @param step 自定义步骤对象
     * @return 返回自身，支持链式调用
     */
    fun step(step: TaskStep<*, *>): TaskFlowBuilder {
        steps.add(step)
        return this
    }

    /** 构建 [TaskFlowEngine] */
    fun build(): TaskFlowEngine = TaskFlowEngine(steps)
}

/**
 * DSL 入口函数，用于创建任务流引擎
 *
 * @param block 构建器 DSL
 * @return [TaskFlowEngine] 构建完成的任务流引擎
 */
fun taskFlowEngine(block: TaskFlowBuilder.() -> Unit): TaskFlowEngine {
    val builder = TaskFlowBuilder()
    builder.block()
    return builder.build()
}