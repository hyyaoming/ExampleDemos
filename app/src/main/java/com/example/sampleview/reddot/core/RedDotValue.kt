package com.example.sampleview.reddot.core

/**
 * 红点状态的封装类，表示红点显示的具体内容和类型。
 *
 * 通过密封类 [RedDotValue]，定义三种常见的红点状态：
 * - [Empty]：无红点，不显示。
 * - [Dot]：普通小红点，仅表示存在未读状态。
 * - [Number]：数字红点，显示具体数量。
 *
 * 所有子类必须实现 [shouldShow] 方法，用于判断该状态是否需要在界面上显示红点。
 */
sealed class RedDotValue {

    /**
     * 判断当前红点状态是否应该被显示。
     *
     * @return true 表示需要显示红点，false 表示不显示。
     */
    abstract fun shouldShow(): Boolean

    /**
     * 空红点状态，表示不显示任何红点。
     */
    object Empty : RedDotValue() {
        override fun shouldShow() = false
        override fun toString() = "Empty"
    }

    /**
     * 普通小红点状态，仅表示有未读/未处理事项，
     * 不显示具体数量。
     */
    object Dot : RedDotValue() {
        override fun shouldShow() = true
        override fun toString() = "Dot"
    }

    /**
     * 数字红点状态，显示一个整数计数，
     * 通常表示未读消息数、通知数等。
     *
     * @property count 红点上显示的数字，必须大于 0 才显示。
     */
    data class Number(val count: Int) : RedDotValue() {
        override fun shouldShow() = count > 0
        override fun toString() = "Number($count)"
    }
}
