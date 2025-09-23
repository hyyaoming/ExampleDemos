package com.example.sampleview.eventtracker.plugin

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventResult

/**
 * 事件插件接口 [EventPlugin]，用于在事件追踪流程的前后执行额外逻辑。
 *
 * ### 使用场景
 * - 日志记录：打印或收集事件信息
 * - 埋点增强：在事件上报前后做额外埋点处理
 * - 性能监控：统计事件处理耗时
 * - 数据加工：修改或补充事件属性
 *
 * ### 生命周期
 * 1. **onEventBeforeTrack**：事件被追踪之前调用
 * 2. **事件上传/入队**（由 Dispatcher 或策略处理）
 * 3. **onEventAfterTrack**：事件追踪完成之后调用
 */
interface EventPlugin {

    /**
     * 在事件被追踪之前调用。
     *
     * 可用于：
     * - 修改事件内容或属性
     * - 过滤掉不需要上报的事件
     * - 执行日志或埋点前置操作
     *
     * 默认实现为空，子类可选择性重写。
     *
     * @param event 当前即将被追踪的事件
     */
    suspend fun onEventBeforeTrack(event: Event) {}

    /**
     * 在事件追踪完成之后调用。
     *
     * 可用于：
     * - 上报完成后的日志记录或统计
     * - 事件结果分析（成功、失败、入队）
     * - 后置业务逻辑执行（如触发回调或更新状态）
     *
     * 默认实现为空，子类可选择性重写。
     *
     * @param event 已追踪的事件
     * @param result 事件上报结果，包含成功、失败或入队状态
     */
    suspend fun onEventAfterTrack(event: Event, result: EventResult) {}
}
