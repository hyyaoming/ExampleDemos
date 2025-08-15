package com.example.sampleview.reddot.core

/**
 * 用于逐步构建 [RedDotKey] 的辅助类，支持链式添加路径段，最终构造完整的红点键。
 *
 * @property segments 当前已添加的路径段列表
 */
class RedDotKeyBuilder(private val segments: List<String> = emptyList()) {

    /**
     * 追加一个路径段，返回包含新路径段的全新 [RedDotKeyBuilder] 实例。
     *
     * @param name 路径段名称，不能为空且应符合红点路径规范
     * @return 新的 [RedDotKeyBuilder]，包含追加后的路径段
     */
    fun segment(name: String) = RedDotKeyBuilder(segments + name)

    /**
     * 根据当前路径段构建完整的 [RedDotKey] 实例。
     *
     * @return 由当前路径段组成的完整 [RedDotKey]
     */
    fun build() = RedDotKey.of(*segments.toTypedArray())
}