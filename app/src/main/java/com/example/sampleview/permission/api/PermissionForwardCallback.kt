package com.example.sampleview.permission.api

import android.content.Context

/**
 * 权限跳转回调接口。
 *
 * 当权限被永久拒绝（即用户勾选“不再询问”）时调用此接口方法，
 * 业务通过该回调引导用户跳转到系统设置页面，手动开启权限。
 */
interface PermissionForwardCallback {

    /**
     * 当检测到权限被永久拒绝时调用。
     *
     * @param deniedList 当前被永久拒绝的权限列表，需要用户手动授权。
     * @param context 当前的上下文环境，用于创建 UI 组件或启动设置界面。
     * @param scope 提供跳转设置界面相关的 UI 交互方法，业务可调用显示提示对话框等。
     */
    fun onForwardToSettings(
        deniedList: List<String>,
        context: Context,
        scope: PermissionForwardScope
    )
}
