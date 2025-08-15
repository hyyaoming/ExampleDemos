package com.example.sampleview.permission.api

import android.app.Dialog
import androidx.fragment.app.DialogFragment

/**
 * 权限永久拒绝后，提示用户跳转到应用设置界面的作用域接口。
 *
 * 该接口定义了在用户永久拒绝权限后，
 * 业务层用于引导用户跳转到系统设置页面开启权限的交互方法，
 * 包括展示提示对话框的多种方式。
 */
interface PermissionForwardScope {

    /**
     * 显示跳转设置页的提示对话框。
     *
     * @param permissions 被永久拒绝的权限列表，用于展示给用户。
     * @param message 提示信息，说明为何需要跳转设置页开启权限。
     * @param positiveText 确认按钮文本，用户点击后会跳转到系统设置。
     * @param negativeText 可选的取消按钮文本，用户点击后关闭对话框，不跳转。
     */
    fun showForwardToSettingsDialog(
        permissions: List<String>,
        message: String,
        positiveText: String,
        negativeText: String? = null
    )

    /**
     * 使用自定义 [Dialog] 构建并显示跳转设置页提示对话框。
     *
     * 允许业务传入自定义的对话框实例，满足个性化 UI 需求。
     *
     * @param buildDialog 返回自定义 [Dialog] 的工厂方法。
     */
    fun showForwardToSettingsDialog(buildDialog: () -> Dialog)

    /**
     * 使用自定义 [DialogFragment] 构建并显示跳转设置页提示对话框。
     *
     * 允许业务传入自定义的 [DialogFragment]，实现更灵活的界面交互。
     *
     * @param buildDialogFragment 返回自定义 [DialogFragment] 的工厂方法。
     */
    fun showForwardToSettingsFragmentDialog(buildDialogFragment: () -> DialogFragment)
}
