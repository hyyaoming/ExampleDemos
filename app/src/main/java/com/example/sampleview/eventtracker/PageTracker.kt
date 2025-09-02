package com.example.sampleview.eventtracker

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 页面访问记录数据类
 *
 * @property pageName 页面名称
 * @property lastResumedId 当前前台 Activity 唯一标识（hashCode），用于判定页面切换，避免 Dialog/Popup 干扰
 */
data class PageVisit(
    val pageName: String,
    val lastResumedId: Int,
)

/**
 * Activity 页面访问路径追踪器（可靠版）
 *
 * 功能：
 * 1. 记录用户访问的页面顺序，保证完整路径，例如 "A->B->A->C"
 * 2. 通过 Activity 实例 hashCode 判定切换，避免 DialogFragment / PopupWindow 干扰
 * 3. 记录返回栈访问，连续打开同类不同实例也不会漏掉
 */
object ActivityPathTracker : Application.ActivityLifecycleCallbacks {

    /** 页面访问记录列表（线程安全） */
    private val activityStack = CopyOnWriteArrayList<PageVisit>()

    /**
     * 初始化生命周期回调
     *
     * @param application Application 对象，用于注册 Activity 生命周期监听
     */
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * 获取用户完整页面浏览路径
     *
     * @return 页面路径字符串，例如 "A->B->A->C"
     */
    fun getPath(): String = activityStack.joinToString("->") { it.pageName }

    /**
     * Activity 可见（前台）时调用
     *
     * @param activity 当前可见的 Activity
     *
     * 逻辑：
     * 1. 获取页面名字和实例 hashCode
     * 2. 对比上一次记录的 Activity 实例（lastResumedId）
     * 3. 如果不同，则说明页面切换，记录访问
     * 4. 相同则可能是 Dialog 弹出或后台切回前台，不记录
     */
    override fun onActivityResumed(activity: Activity) {
        val pageName = activity::class.java.simpleName ?: "Unknown"
        val instanceId = activity.hashCode()
        if (activityStack.lastOrNull()?.lastResumedId != instanceId) {
            activityStack.add(PageVisit(pageName, instanceId))
        }
    }

    /**
     * Activity 暂停（不可见）时调用
     *
     * @param activity 当前暂停的 Activity
     * 说明：此处不做记录，仅为生命周期回调占位，可扩展
     */
    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}









