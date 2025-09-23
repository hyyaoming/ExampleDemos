package com.example.sampleview.eventtracker.strategy

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.UploadMode

/**
 * 上传策略注册中心接口。
 *
 * 负责管理不同上传模式（UploadMode）对应的事件上传策略（EventUploadStrategy）。
 * 提供注册、查询、遍历和注销策略的能力。
 *
 * ### 核心职责
 * 1. 注册上传策略并与上传模式关联；
 * 2. 根据事件自动选择对应的策略进行上传；
 * 3. 提供策略集合遍历接口，用于批量刷新或恢复事件；
 * 4. 支持策略的动态注册和反注册。
 */
interface UploadStrategyRegistry {

    /**
     * 获取所有已注册的上传策略。
     *
     * - 返回的 Map 中 key 为 [UploadMode]，value 为对应的 [EventUploadStrategy]。
     * - 可用于批量刷新、恢复事件或管理策略状态。
     *
     * @return Map<UploadMode, EventUploadStrategy> 所有已注册策略的快照
     */
    fun all(): Map<UploadMode, EventUploadStrategy>

    /**
     * 根据事件查找对应的上传策略。
     *
     * - 通过事件的上传模式（event.uploadMode）匹配策略；
     * - 若未注册对应策略，可抛出异常或返回默认策略（由实现决定）。
     *
     * @param event 待上传事件
     * @return [EventUploadStrategy] 匹配的上传策略
     */
    fun findUploadStrategy(event: Event): EventUploadStrategy

    /**
     * 注册或更新一个上传模式对应的策略。
     *
     * - 若上传模式已存在策略，则覆盖原有策略；
     * - 支持动态增加新的上传策略。
     *
     * @param mode 上传模式
     * @param strategy 对应的上传策略
     */
    fun register(mode: UploadMode, strategy: EventUploadStrategy)

    /**
     * 反注册一个上传模式对应的策略。
     *
     * - 移除指定上传模式对应的策略；
     * - 用于策略热更新或动态关闭某类事件上传。
     *
     * @param mode 上传模式
     */
    fun unregister(mode: UploadMode)
}
