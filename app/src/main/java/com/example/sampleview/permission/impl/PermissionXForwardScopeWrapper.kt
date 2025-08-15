package com.example.sampleview.permission.impl

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import com.example.sampleview.permission.api.PermissionForwardScope
import com.permissionx.guolindev.dialog.RationaleDialog
import com.permissionx.guolindev.dialog.RationaleDialogFragment
import com.permissionx.guolindev.request.ForwardScope

/**
 * PermissionForwardScope 接口的 PermissionX 实现适配器。
 *
 * 该类将 PermissionX 库中的 ForwardScope 包装为统一的 PermissionForwardScope 接口，
 * 以便业务代码通过统一接口调用，隐藏底层 PermissionX 具体实现细节。
 *
 * @param pxScope PermissionX 提供的 ForwardScope 实例，用于执行具体权限跳转设置页的提示操作。
 */
class PermissionXForwardScopeWrapper(private val pxScope: ForwardScope) : PermissionForwardScope {

    /**
     * 显示跳转设置页的提示对话框，调用 PermissionX 的对应方法。
     *
     * @param permissions 被永久拒绝的权限列表。
     * @param message 提示信息文本，说明为何需要跳转设置页。
     * @param positiveText 确认按钮文字。
     * @param negativeText 取消按钮文字，可为空。
     */
    override fun showForwardToSettingsDialog(
        permissions: List<String>,
        message: String,
        positiveText: String,
        negativeText: String?
    ) {
        pxScope.showForwardToSettingsDialog(permissions, message, positiveText, negativeText)
    }

    /**
     * 使用自定义 Dialog 展示跳转设置页的提示。
     *
     * @param buildDialog 构建 Dialog 实例的函数。
     *                    仅当 Dialog 是 PermissionX 的 RationaleDialog 类型时调用底层方法。
     */
    override fun showForwardToSettingsDialog(buildDialog: () -> Dialog) {
        val dialog = buildDialog()
        if (dialog is RationaleDialog) {
            pxScope.showForwardToSettingsDialog(dialog)
        }
    }

    /**
     * 使用自定义 DialogFragment 展示跳转设置页的提示。
     *
     * @param buildDialogFragment 构建 DialogFragment 实例的函数。
     *                           仅当 DialogFragment 是 PermissionX 的 RationaleDialogFragment 类型时调用底层方法。
     */
    override fun showForwardToSettingsFragmentDialog(buildDialogFragment: () -> DialogFragment) {
        val dialogFragment = buildDialogFragment()
        if (dialogFragment is RationaleDialogFragment) {
            pxScope.showForwardToSettingsDialog(dialogFragment)
        }
    }
}
