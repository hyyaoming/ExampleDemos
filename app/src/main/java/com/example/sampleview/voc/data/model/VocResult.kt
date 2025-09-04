package com.example.sampleview.voc.data.model

sealed class VocResult {

    /** 用户成功提交问卷 */
    data class Success(val params: Answer) : VocResult() {
        override fun toString(): String = "Success(params=$params)"
    }

    /** 用户取消反馈 */
    data class Cancelled(val reason: String) : VocResult() {
        override fun toString(): String = "Cancelled reason:$reason"
    }

    /** 问卷已经展示过，根据策略判断无需再次展示 */
    data object AlreadyShown : VocResult() {
        override fun toString(): String = "AlreadyShown"
    }
}
