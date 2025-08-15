package com.example.sampleview.permission.impl

import android.app.Dialog
import androidx.fragment.app.DialogFragment
import com.example.sampleview.permission.api.PermissionExplainScope
import com.permissionx.guolindev.dialog.RationaleDialog
import com.permissionx.guolindev.dialog.RationaleDialogFragment
import com.permissionx.guolindev.request.ExplainScope

/**
 * PermissionExplainScope 接口的 PermissionX 实现适配器。
 *
 * 该类将 PermissionX 库中的 ExplainScope 包装为统一的 PermissionExplainScope 接口，
 * 以便业务代码使用统一接口调用，隐藏底层 PermissionX 具体实现细节。
 *
 * @param pxScope PermissionX 提供的 ExplainScope 实例，用于执行具体权限解释弹窗操作。
 */
class PermissionXExplainScopeWrapper(
    private val pxScope: ExplainScope
) : PermissionExplainScope {

    /**
     * 显示权限请求理由对话框，调用 PermissionX 的对应方法。
     *
     * @param permissions 被拒绝的权限列表。
     * @param message 权限解释信息文本。
     * @param positiveText 确认按钮文字。
     * @param negativeText 取消按钮文字，可为空。
     */
    override fun showRequestReasonDialog(
        permissions: List<String>,
        message: String,
        positiveText: String,
        negativeText: String?
    ) {
        pxScope.showRequestReasonDialog(permissions, message, positiveText, negativeText)
    }

    /**
     * 使用自定义 Dialog 展示权限请求理由。
     *
     * @param buildDialog 构建 Dialog 实例的函数。
     *                    仅当 Dialog 是 PermissionX 的 RationaleDialog 类型时调用底层方法。
     */
    override fun showRequestReasonDialog(buildDialog: () -> Dialog) {
        val dialog = buildDialog()
        if (dialog is RationaleDialog) {
            pxScope.showRequestReasonDialog(dialog)
        }
    }

    /**
     * 使用自定义 DialogFragment 展示权限请求理由。
     *
     * @param buildDialogFragment 构建 DialogFragment 实例的函数。
     *                           仅当 DialogFragment 是 PermissionX 的 RationaleDialogFragment 类型时调用底层方法。
     */
    override fun showRequestReasonDialogFragment(buildDialogFragment: () -> DialogFragment) {
        val dialogFragment = buildDialogFragment()
        if (dialogFragment is RationaleDialogFragment) {
            pxScope.showRequestReasonDialog(dialogFragment)
        }
    }
}
