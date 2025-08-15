package com.example.sampleview.reddot.core

/**
 * 用于标识红点（RedDot）附加信息的键对象。
 *
 * 该类是类型安全的泛型键，支持以 Map<RedDotExtraKey<*>, Any> 的形式存储红点的附加信息。
 * 例如：点击来源、用户行为上下文、展示位置等元信息，均可通过该 Key 存储和访问。
 *
 * @param T 与该 Key 对应的附加信息类型。例如 RedDotExtraKey<Int> 表示附加值应为 Int。
 * @property name 键的唯一标识名称，用于区分不同的 Key。
 */
class RedDotExtraKey<T>(val name: String) {

    /**
     * 返回该 Key 的字符串表示，用于日志或调试目的。
     */
    override fun toString(): String = "RedDotExtraKey($name)"

    /**
     * 判断两个 Key 是否相等，仅比较名称是否一致，不考虑泛型参数 T。
     */
    override fun equals(other: Any?) = other is RedDotExtraKey<*> && other.name == name

    /**
     * 返回基于名称的哈希值。
     */
    override fun hashCode(): Int = name.hashCode()
}
