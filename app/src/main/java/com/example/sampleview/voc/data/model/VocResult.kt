package com.example.sampleview.voc.data.model

/**
 * 问卷操作结果类型，用于表示问卷流程的最终状态。
 *
 * 包含几种结果：
 * 1. [None]：未获取到问卷问题。
 * 2. [Success]：用户成功提交答案。
 * 3. [Cancelled]：用户取消问卷。
 * 4. [AlreadyShown]：问卷已展示过。
 */
sealed interface VocResult {

    /**
     * 未获取到问卷问题。
     */
    data object None : VocResult {
        override fun toString(): String = "None"
    }

    /**
     * 用户成功提交答案。
     *
     * @property params 用户填写的答案 [Answer]。
     */
    data class Success(val params: Answer) : VocResult {
        override fun toString(): String = "Success(params=$params)"
    }

    /**
     * 用户取消问卷。
     *
     * @property reason 用户取消原因说明。
     */
    data class Cancelled(val reason: String) : VocResult {
        override fun toString(): String = "Cancelled reason:$reason"
    }

    /**
     * 问卷已展示过，不需要重复展示。
     */
    data object AlreadyShown : VocResult {
        override fun toString(): String = "AlreadyShown"
    }
}
