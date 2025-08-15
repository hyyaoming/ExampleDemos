package com.example.sampleview.permission.api

import android.content.Context

/**
 * 权限解释回调接口。
 *
 * 当用户首次拒绝权限请求时，需要向用户解释权限用途，以提升授权率时调用此接口。
 * 业务层应在此回调中，使用提供的 [PermissionExplainScope] 显示权限申请理由的 UI（例如弹窗），
 * 并引导用户继续授权流程。
 */
interface PermissionExplainCallback {

    /**
     * 触发向用户解释权限请求理由的方法。
     *
     * @param deniedList 当前被拒绝的权限列表，方便显示具体权限用途说明。
     * @param context 当前请求权限的上下文，通常为 Activity 或 Fragment 的 Context，用于创建 UI。
     * @param scope 用于显示解释理由的作用域接口，业务层通过它展示自定义弹窗或提示界面。
     */
    fun onExplainRequestReason(
        deniedList: List<String>,
        context: Context,
        scope: PermissionExplainScope
    )
}
