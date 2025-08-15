package com.example.sampleview

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference

@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
object AppActivityManager : Application.ActivityLifecycleCallbacks {

    private val activityStack = mutableListOf<Activity>()
    private var _currentActivityRef: WeakReference<Activity>? = null
    private var activityCount = 0

    /** 当前 Activity */
    val currentActivity: Activity?
        get() = _currentActivityRef?.get()

    /** 当前页面类名（路径） */
    val currentActivityName: String
        get() = currentActivity?.javaClass?.simpleName ?: ""

    /** 应用是否在前台 */
    val isAppInForeground: Boolean
        get() = activityCount > 0

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

    // ---------------------- 生命周期回调 ----------------------

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activityStack.add(activity)
    }

    override fun onActivityStarted(activity: Activity) {
        activityCount++
    }

    override fun onActivityResumed(activity: Activity) {
        _currentActivityRef = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {
        activityCount--
    }

    override fun onActivityDestroyed(activity: Activity) {
        activityStack.remove(activity)
        if (_currentActivityRef?.get() == activity) {
            _currentActivityRef = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}