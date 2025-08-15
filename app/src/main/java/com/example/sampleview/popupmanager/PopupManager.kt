package com.example.sampleview.popupmanager

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.PriorityQueue
import kotlin.coroutines.cancellation.CancellationException

/**
 * 弹窗管理器，用于按优先级顺序依次展示各种弹窗（AlertDialog、DialogFragment）。
 *
 * ## 特性
 * - 支持弹窗队列管理，高优先级弹窗先展示。
 * - 挂起显示每个弹窗，保证上一个弹窗消失后再展示下一个。
 * - 支持通过 tag 取消指定弹窗或清空队列。
 * - 弹窗显示与关闭均在 Main 线程执行，保证 UI 操作安全。
 *
 * @param scope 用于启动协程的 CoroutineScope，通常传 Activity/Fragment 的 lifecycleScope。
 */
class PopupManager(private val scope: CoroutineScope) {

    /** 弹窗优先队列，按 priority 降序排列 */
    private val popupQueue = PriorityQueue<PopupController>(compareByDescending { it.priority })

    /** 当前正在显示的弹窗 */
    private var currentPopup: PopupController? = null

    /** 弹窗消费协程 Job，用于按队列顺序依次展示弹窗 */
    private var consumerJob: Job? = null

    /**
     * 将弹窗加入队列并启动消费协程。
     *
     * @param context 弹窗所在的 Context，通常为 Activity
     * @param popup 待显示的弹窗
     */
    fun enqueue(context: Context, popup: PopupController) {
        scope.launch(Dispatchers.Main) {
            if (context !is Activity || context.isFinishing || context.isDestroyed) return@launch
            popupQueue.offer(popup)
            startConsumer(context)
        }
    }

    /**
     * 启动弹窗消费协程，如果已经在运行则不重复启动。
     *
     * 内部循环从队列中依次取出弹窗，并挂起显示，
     * 直到队列为空或协程被取消。
     */
    private fun startConsumer(context: Context) {
        if (consumerJob?.isActive == true) return

        consumerJob = scope.launch(Dispatchers.Main) {
            while (popupQueue.isNotEmpty()) {
                val popup = popupQueue.poll() ?: break
                currentPopup = popup
                try {
                    popup.show(context)
                } catch (_: CancellationException) {
                    popup.dismiss()
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    currentPopup = null
                }
            }
        }
    }

    /**
     * 取消指定 tag 的弹窗。
     *
     * 如果弹窗在队列中，会从队列中移除；
     * 如果当前正在显示，会立即关闭。
     *
     * @param tag 待取消弹窗的唯一标识
     */
    fun cancel(tag: String) {
        scope.launch(Dispatchers.Main) {
            popupQueue.removeAll { it.tag == tag }
            if (currentPopup?.tag == tag) {
                currentPopup?.dismiss()
                currentPopup = null
            }
        }
    }

    /**
     * 清空弹窗队列并关闭当前正在显示的弹窗。
     */
    fun clearQueue() {
        scope.launch(Dispatchers.Main) {
            consumerJob?.cancel()
            consumerJob = null
            popupQueue.clear()
            currentPopup?.dismiss()
            currentPopup = null
        }
    }
}
