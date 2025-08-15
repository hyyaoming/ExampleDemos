package com.example.sampleview.permission.impl

import android.content.Context
import com.example.sampleview.permission.api.PermissionDescriptionResolver
import com.example.sampleview.permission.api.PermissionLifecycleSlice
import com.example.sampleview.permission.model.PermissionResult
import com.example.sampleview.permission.ui.CommonPermissionFloatView
import com.example.sampleview.permission.util.findActivity
import com.example.sampleview.permission.util.removeSelfFromParent
import com.example.sampleview.permission.util.runSafeDelayJob
import java.lang.ref.WeakReference

/**
 * 权限请求生命周期切片，负责在请求前后展示简要的UI提示。
 *
 * @property descResolver 权限描述解析器，用于获取权限的详细说明
 */
class PermissionUiHintSlice(private val descResolver: PermissionDescriptionResolver) : PermissionLifecycleSlice {
    private var floatViewRef: WeakReference<CommonPermissionFloatView?>? = null

    /**
     * 请求权限之前调用，展示请求权限的简要说明提示。
     *
     * @param permissions 当前请求的权限列表
     * @param context 当前上下文环境
     * @return 返回 true 允许继续请求权限
     */
    override fun onBeforeRequest(permissions: List<String>, context: Context) {
        releaseFloatView()
        val floatView = CommonPermissionFloatView.attachToDecorView(context.findActivity())
        val desc = descResolver.resolve(permissions)
        floatView?.bindPermissionDesc(desc.firstOrNull())
        floatViewRef = WeakReference(floatView)
    }

    /**
     * 请求权限完成后调用，展示权限请求结束的提示。
     *
     * @param result 权限请求结果
     * @param context 当前上下文环境
     */
    override fun onAfterRequest(result: PermissionResult, context: Context) {
        super.onAfterRequest(result, context)
        context.findActivity()?.runSafeDelayJob {
            releaseFloatView()
        }
    }

    private fun releaseFloatView() {
        floatViewRef?.get()?.removeSelfFromParent()
        floatViewRef = null
    }
}
