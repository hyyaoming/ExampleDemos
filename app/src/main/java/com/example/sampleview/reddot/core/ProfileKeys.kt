package com.example.sampleview.reddot.core

/**
 * 封装 profile 模块中所有相关红点键，方便统一管理和访问。
 *
 * @property builder 内部使用的 [RedDotKeyBuilder]，默认以 "profile" 作为根路径段
 */
class ProfileKeys(private val builder: RedDotKeyBuilder = RedDotKeyBuilder(listOf("profile"))) {

    val chatMessage by lazy { builder.segment("chatMessage") }
    val updateApp by lazy { builder.segment("updateApp") }

    /**
     * profile 模块的根路径字符串，通常用于聚合该模块所有红点数据。
     */
    val root by lazy { builder.build().fullKey }

    /**
     * profile/feedbackUnRead 的路径前缀，用于聚合用户反馈相关的红点数据。
     */
    val feedbackUnReadPrefix by lazy { builder.segment("feedbackUnRead").build().fullKey }

    /**
     * 构造指定用户反馈 ID 的红点键。
     *
     * @param feedbackId 具体的反馈唯一标识
     * @return 针对该反馈的完整 [RedDotKey]
     */
    fun feedBackUnRead(feedbackId: String) = builder.segment("feedbackUnRead").segment(feedbackId).build()
}