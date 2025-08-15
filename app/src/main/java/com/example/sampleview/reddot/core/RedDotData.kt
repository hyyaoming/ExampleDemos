package com.example.sampleview.reddot.core

/**
 * 红点数据模型，表示某个红点项的具体状态与附加信息。
 *
 * @property key 红点标识符，唯一定位一个红点项。
 * @property redDotValue 红点的当前值，如未读状态、计数值等。
 * @property extras 附加信息，可选，用于存储额外的业务扩展数据（如时间戳、来源等），
 *                  通过 [RedDotExtraKey] 类型安全地访问。
 */
data class RedDotData(
    val key: RedDotKey,
    val redDotValue: RedDotValue,
    private val extras: Map<RedDotExtraKey<*>, Any> = emptyMap(),
) {
    /**
     * 以类型安全的方式获取附加字段。
     *
     * @param key [RedDotExtraKey] 类型键。
     * @return 存储在 extras 中的值，若不存在或类型不匹配则返回 null。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getExtra(key: RedDotExtraKey<T>): T? = extras[key] as? T

    /**
     * 创建一个新的 [RedDotData] 实例，增加或覆盖一个附加字段。
     *
     * @param key 附加字段的 key。
     * @param value 附加字段的值。
     * @return 返回包含新 extras 的副本。
     */
    fun <T> withExtra(key: RedDotExtraKey<T>, value: T): RedDotData =
        copy(extras = extras + (key to value as Any))

    /**
     * 使用 block 批量修改 extras 字段，返回新的 [RedDotData]。
     *
     * @param block 接收一个可变 map，支持对附加字段进行批量添加或修改。
     * @return 返回包含修改后 extras 的副本。
     */
    fun withExtras(block: MutableMap<RedDotExtraKey<*>, Any>.() -> Unit): RedDotData {
        val newExtras = extras.toMutableMap().apply(block)
        return copy(extras = newExtras)
    }

    /**
     * 添加多个附加字段，返回新的 [RedDotData] 实例。
     *
     * @param pairs 若干键值对形式的附加字段。
     * @return 返回包含新增字段的副本。
     */
    fun withExtras(vararg pairs: Pair<RedDotExtraKey<*>, Any>): RedDotData =
        copy(extras = extras + pairs)

    /**
     * 获取所有附加字段。
     *
     * @return extras 的不可变副本。
     */
    fun getAllExtras(): Map<RedDotExtraKey<*>, Any> = extras.toMap()
}

