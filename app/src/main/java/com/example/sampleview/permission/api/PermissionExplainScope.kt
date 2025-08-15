package com.example.sampleview.permission.api

import android.app.Dialog
import androidx.fragment.app.DialogFragment

/**
 * 权限解释范围接口。
 *
 * 用于在权限请求被拒绝时，向用户展示权限请求原因的对话框或提示界面。
 * 业务实现该接口以提供自定义的权限请求理由展示方式，
 * 帮助用户理解为什么需要授予特定权限，从而提升授权通过率。
 */
interface PermissionExplainScope {

    /**
     * 显示权限请求理由的对话框。
     *
     * @param deniedPermissions 当前被拒绝的权限列表，用于说明具体权限用途。
     * @param message 向用户展示的说明信息，描述为何需要这些权限。
     * @param positiveText 对话框中确认按钮的文本，点击后应继续请求权限。
     * @param negativeText 可选参数，对话框中取消按钮的文本，用户点击后取消请求。
     */
    fun showRequestReasonDialog(
        deniedPermissions: List<String>,
        message: String,
        positiveText: String,
        negativeText: String? = null
    )

    /**
     * 通过构建器方式显示自定义的权限请求理由对话框。
     *
     * @param buildDialog 返回自定义的 [Dialog] 实例。
     */
    fun showRequestReasonDialog(buildDialog: () -> Dialog)

    /**
     * 通过构建器方式显示自定义的权限请求理由对话框片段。
     *
     * @param buildDialogFragment 返回自定义的 [DialogFragment] 实例。
     */
    fun showRequestReasonDialogFragment(buildDialogFragment: () -> DialogFragment)
}
