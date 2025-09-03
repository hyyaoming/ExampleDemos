package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.EventTrackerConfig
import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.UploadMode
import java.util.concurrent.ConcurrentHashMap

/**
 * 事件上传策略注册中心。
 *
 * 负责维护不同上传模式 ([UploadMode]) 对应的 [EventUploadStrategy]。
 * - 支持注册和注销策略
 * - 支持根据事件查找对应的上传策略
 *
 * @param uploadConfig 上传配置，用于初始化默认策略
 */
class EventUploadStrategyRegistry(uploadConfig: EventTrackerConfig.UploaderConfig) : UploadStrategyRegistry {

    /** 已注册的上传策略映射表，线程安全 */
    private val strategies = ConcurrentHashMap<UploadMode, EventUploadStrategy>()

    init {
        register(UploadMode.IMMEDIATE, ImmediateUploadStrategy(uploadConfig))
        register(UploadMode.BATCH, BatchUploadStrategy(uploadConfig))
    }

    /**
     * 获取所有已注册的上传策略。
     *
     * @return Map<UploadMode, EventUploadStrategy> 当前注册的策略映射
     */
    override fun all(): Map<UploadMode, EventUploadStrategy> = strategies

    /**
     * 根据事件的上传模式查找对应的策略。
     *
     * @param event 待查找策略的事件
     * @return 对应的 [EventUploadStrategy]，若未注册返回 null
     */
    override fun findUploadStrategy(event: Event): EventUploadStrategy? = strategies[event.uploadMode]

    /**
     * 注册一个上传模式对应的策略。
     *
     * - 如果该模式已存在，会覆盖原有策略
     *
     * @param mode 上传模式
     * @param strategy 对应的上传策略实现
     */
    override fun register(mode: UploadMode, strategy: EventUploadStrategy) {
        strategies[mode] = strategy
    }

    /**
     * 注销指定上传模式对应的策略。
     *
     * - 如果该模式未注册，则无操作
     *
     * @param mode 需要注销的上传模式
     */
    override fun unregister(mode: UploadMode) {
        strategies.remove(mode)
    }
}
