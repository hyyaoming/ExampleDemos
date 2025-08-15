package com.xnhz.libbase.dialog

/**
 * LoadingHandler 接口定义了统一的加载弹窗操作方法。
 * 实现类负责具体的加载弹窗展示和关闭逻辑。
 */
interface LoadingHandler {

    /**
     * 显示加载弹窗。
     *
     * @param tipWord    弹窗显示的提示文字，默认为空字符串。
     * @param cancelable 弹窗是否可以被取消，默认为 true。
     * @param minDuration 最短展示时长（毫秒），默认 0 表示不限制
     *
     * 注意：
     * - 该方法应在主线程调用。
     * - 实现时应避免重复展示弹窗。
     */
    fun showTipLoading(tipWord: String = "", cancelable: Boolean = true, minDuration: Long = 0L)

    /**
     * 关闭加载弹窗。
     *
     * 注意：
     * - 该方法应在主线程调用。
     * - 实现时应安全地处理弹窗已关闭或不存在的情况。
     */
    fun dismissTipLoading()

    /**
     * 判断加载弹窗是否正在显示。
     *
     * @return 返回 true 表示弹窗当前正在显示，false 表示未显示。
     */
    fun isTipLoadingShowing(): Boolean
}
