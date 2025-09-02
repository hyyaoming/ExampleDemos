package com.example.sampleview.eventtracker.plugin

import com.example.sampleview.eventtracker.model.Event
import com.example.sampleview.eventtracker.model.EventUploadResult

/**
 * 事件插件接口，用于在事件追踪前后执行额外逻辑。
 *
 * 可以用于日志、埋点增强、性能监控等业务需求。
 */
interface EventPlugin {

    /**
     * 在事件被追踪之前调用。
     *
     * @param event 当前即将被追踪的事件
     */
    suspend fun onEventBeforeTrack(event: Event) {}

    /**
     * 在事件追踪完成之后调用。
     *
     * @param event 已追踪的事件
     * @param result 事件上报结果，包含成功、失败或入队状态
     */
    suspend fun onEventAfterTrack(event: Event, result: EventUploadResult) {}
}
