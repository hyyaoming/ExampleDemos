package com.xnhz.libbase.dialog

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.example.sampleview.AppLogger
import com.example.sampleview.loading.TipDialog
import java.lang.ref.WeakReference

/**
 * 基于 [TipDialog] 的加载弹窗处理器，实现 [LoadingHandler] 接口。
 *
 * 特性说明：
 * - 通过弱引用持有 [Activity]，防止内存泄漏；
 * - 支持通过参数设置“最小展示时长”，防止弹窗一闪而过导致用户无感知；
 * - 所有 UI 操作都要求在主线程执行，若非主线程调用将被安全忽略并记录日志；
 *
 * 使用方式：
 * ```
 * loadingHandler.showTipLoading("加载中...", cancelable = false, minDuration = 800L)
 * loadingHandler.dismissTipLoading() // 若未满800ms，会自动延迟关闭
 * ```
 *
 * @constructor 传入当前有效的 Activity 实例
 */
class TipDialogLoadingHandler(activity: Activity?) : LoadingHandler {

    /**
     * Activity 弱引用，避免持有强引用造成内存泄漏。
     */
    private val activityRef = WeakReference(activity)

    /**
     * 当前显示的加载弹窗实例。
     * 当为 null 时表示弹窗未显示或已被销毁。
     */
    private var tipDialog: TipDialog? = null

    /**
     * 弹窗展示的时间戳（毫秒），用于计算最小展示时间。
     */
    private var showTimestamp: Long = 0L

    /**
     * 本次弹窗设置的最短展示时间（毫秒），默认 0 表示无需强制最短时间。
     */
    private var minDisplayDuration: Long = 0L

    /**
     * 主线程 Handler，用于处理延迟 dismiss。
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 显示加载弹窗。
     *
     * 如果当前弹窗已显示则忽略本次调用。
     * 在调用前会检查 Activity 是否有效（未销毁且未结束）。
     * 该方法要求必须在主线程调用，非主线程调用时将记录错误日志并忽略调用。
     *
     * @param tipWord 弹窗显示的提示文字，默认为空字符串
     * @param cancelable 是否允许用户取消弹窗，默认可取消
     * @param minDuration 最短展示时长（毫秒），默认 0 表示不限制
     */
    override fun showTipLoading(tipWord: String, cancelable: Boolean, minDuration: Long) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            AppLogger.e(TAG, "showTipLoading must be called on the main thread!")
            return
        }
        val activity = activityRef.get()
        if (activity == null) {
            AppLogger.e(TAG, "Activity reference lost, cannot show loading")
            return
        }
        if (tipDialog?.isShowing == true) return
        if (activity.isFinishing || activity.isDestroyed) {
            AppLogger.e(TAG, "Activity is finishing/destroyed, cannot show loading")
            return
        }
        try {
            tipDialog = TipDialog.Builder(activity)
                .setTipWord(tipWord)
                .create(cancelable)
            tipDialog?.show()
            showTimestamp = SystemClock.elapsedRealtime()
            minDisplayDuration = minDuration
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception showing loading dialog", e)
        }
    }

    /**
     * 关闭加载弹窗。
     *
     * 如果弹窗显示时间小于最短显示时长，则延迟关闭。
     * 所有 UI 操作必须在主线程执行。
     */
    override fun dismissTipLoading() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            AppLogger.e(TAG, "dismissTipLoading must be called on the main thread!")
            return
        }

        val elapsed = SystemClock.elapsedRealtime() - showTimestamp
        if (elapsed < minDisplayDuration) {
            val delay = minDisplayDuration - elapsed
            mainHandler.postDelayed({ safelyDismiss() }, delay)
        } else {
            safelyDismiss()
        }
    }

    /**
     * 安全地关闭弹窗，不做线程检查。
     */
    private fun safelyDismiss() {
        try {
            tipDialog?.takeIf { it.isShowing }?.dismiss()
        } catch (e: Exception) {
            AppLogger.e(TAG, "Exception dismissing loading dialog", e)
        }
        tipDialog = null
    }

    /**
     * 判断加载弹窗是否正在显示。
     *
     * @return 如果弹窗存在且正在显示，返回 true；否则返回 false。
     */
    override fun isTipLoadingShowing(): Boolean {
        return tipDialog?.isShowing == true
    }

    companion object {
        private const val TAG = "TipDialogLoadingHandler"
    }
}

