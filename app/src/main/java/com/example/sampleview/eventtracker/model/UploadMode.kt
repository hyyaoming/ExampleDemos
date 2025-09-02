package com.example.sampleview.eventtracker.model

/**
 * 定义事件的上报模式。
 */
enum class UploadMode {

    /**
     * 立即上报模式，事件创建后立刻上传。
     */
    IMMEDIATE,

    /**
     * 批量上报模式，事件先加入队列，等待批量上传。
     */
    BATCH
}
