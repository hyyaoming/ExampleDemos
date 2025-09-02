package com.example.sampleview

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

object AppActivityManager : Application.ActivityLifecycleCallbacks {
    private val listeners = CopyOnWriteArrayList<AppStatusListener>()
    private val activityStack = mutableListOf<Activity>()
    private var _currentActivityRef: WeakReference<Activity>? = null
    private val activityCount = AtomicInteger(0)

    /** 当前 Activity */
    val currentActivity: Activity?
        get() = _currentActivityRef?.get()

    /** 当前页面类名（路径） */
    val currentActivityName: String
        get() = currentActivity?.javaClass?.simpleName ?: ""

    /** 应用是否在前台 */
    val isAppInForeground: Boolean
        get() = activityCount.get() > 0

    fun getActivityStackSize(): Int {
        return activityStack.size
    }

    /** 注册生命周期回调 */
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /** 获取 Activity 栈顶 */
    fun topActivity(): Activity? = activityStack.lastOrNull()

    /** 关闭指定 Activity */
    fun finishActivity(clazz: Class<out Activity>) {
        activityStack.filter { it.javaClass == clazz }
            .forEach { it.finish() }
        activityStack.removeAll { it.javaClass == clazz }
    }

    /** 关闭所有 Activity */
    fun finishAllActivities() {
        activityStack.forEach { it.finish() }
        activityStack.clear()
    }

    fun finishAllActivitiesWithout(clazz: Class<out Activity>) {
        val iterator = activityStack.iterator()
        while (iterator.hasNext()) {
            val activity = iterator.next()
            if (activity.javaClass != clazz) {
                activity.finish() // 先 finish
                iterator.remove() // 再从列表移除
            }
        }
    }

    /** 某个 Activity 是否存在 */
    fun isActivityAlive(clazz: Class<out Activity>): Boolean {
        return activityStack.any { it.javaClass == clazz && !it.isFinishing }
    }

    /** 打印 Activity 栈 */
    fun logActivityStack() {
        Log.d("AppActivityManager", "Activity Stack:")
        activityStack.forEachIndexed { index, activity ->
            Log.d("AppActivityManager", "[$index] ${activity::class.java.simpleName}")
        }
    }

    fun registerAppStatusListener(listener: AppStatusListener) {
        listeners.addIfAbsent(listener)
    }

    fun unRegisterAppStatusListener(listener: AppStatusListener) {
        listeners.remove(listener)
    }

    // ---------------------- 生命周期回调 ----------------------

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityStack.add(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        val count = activityCount.incrementAndGet()
        if (count == 1) {
            listeners.forEach { it.onForeground() }
            AppLogger.d("AppActivityManager", "App is in foreground")
        }
    }

    override fun onActivityResumed(activity: Activity) {
        _currentActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        val count = activityCount.decrementAndGet()
        if (count == 0) {
            listeners.forEach { it.onBackground() }
            AppLogger.d("AppActivityManager", "App is in background")
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityStack.remove(activity)
        if (_currentActivityRef?.get() == activity) {
            _currentActivityRef = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}

/**
 * App状态监听
 */
interface AppStatusListener {
    /**
     * 前台
     */
    fun onForeground()

    /**
     * 后台
     */
    fun onBackground()
}
