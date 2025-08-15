package com.example.sampleview.permission.api

import android.content.Context
import com.example.sampleview.permission.model.PermissionResult

/**
 * 权限请求生命周期切片接口
 *
 * 用于处理权限请求的生命周期事件，
 * 例如请求前的准备工作、请求完成后的处理，以及权限描述解析器的更新。
 */
interface PermissionLifecycleSlice {

    /**
     * 请求权限之前调用。若返回 false，则中断权限请求。
     *
     * @param permissions 本次请求的权限列表
     * @param context 当前上下文环境
     * @return 是否继续执行权限请求，false 表示中断
     */
    fun onBeforeRequest(permissions: List<String>, context: Context)

    /**
     * 权限请求完成后的回调方法。
     *
     * @param result 权限请求结果
     * @param context 当前上下文环境
     */
    fun onAfterRequest(result: PermissionResult, context: Context) {}
}
