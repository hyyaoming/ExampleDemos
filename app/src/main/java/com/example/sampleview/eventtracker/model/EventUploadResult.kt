package com.example.sampleview.eventtracker.model

/**
 * 表示事件上传操作的结果。
 *
 * 使用封闭类 [sealed class] 封装不同类型的上传结果，
 * 包括成功、失败、空操作或被跳过的情况。
 */
sealed class EventUploadResult {

    /**
     * 成功上传事件。
     *
     * @property events 已成功处理的事件列表
     */
    data class Success(val events: List<Event>) : EventUploadResult() {
        data class Success(val events: List<Event>) : EventUploadResult() {
            override fun toString(): String {
                return "Success(events=[${events.joinToString { it.toString() }}])"
            }
        }
    }

    /**
     * 事件已入队等待批量上传，但尚未上传
     *
     * @property events 进入队列的事件
     */
    data class Queued(val events: List<Event>) : EventUploadResult() {
        override fun toString(): String {
            return "Queued(events=[${events.joinToString { it.toString() }}])"
        }
    }

    /**
     * 没有事件被处理。
     *
     * 通常表示：
     * - 批量上传队列未满
     * - 立即上传策略的空操作
     */
    object Empty : EventUploadResult() {
        override fun toString(): String {
            return "Empty"
        }
    }

    /**
     * 上传失败的事件及异常信息。
     *
     * @property events 处理失败的事件列表
     * @property throwable 导致上传失败的异常
     */
    data class Failure(val events: List<Event>, val throwable: Throwable) : EventUploadResult() {
        override fun toString(): String {
            val eventDetails = events.joinToString(separator = ",\n") { it.toString() }
            return "Failure(events=[\n$eventDetails\n], throwable=${throwable::class.simpleName}: ${throwable.message})"
        }
    }

    /**
     * 事件被策略跳过，不会被上传。
     *
     * 通常表示没有匹配到合适的上传策略。
     */
    object Skipped : EventUploadResult() {
        override fun toString(): String {
            return "Skipped"
        }
    }
}
