package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.UploadMode

/**
 * 上传策略注册中心接口。
 *
 * 提供策略注册、查询和遍历功能，支持线程安全操作。
 */
interface UploadStrategyRegistry {

    /**
     * 获取所有已注册的上传策略。
     */
    fun all(): Map<UploadMode, EventUploadStrategy>

    /**
     * 根据事件查找对应的上传策略。
     */
    fun findUploadStrategy(event: Event): EventUploadStrategy?

    /**
     * 注册或更新一个上传模式对应的策略。
     */
    fun register(mode: UploadMode, strategy: EventUploadStrategy)

    /**
     * 反注册一个上传模式对应的策略
     */
    fun unregister(mode: UploadMode)
}
