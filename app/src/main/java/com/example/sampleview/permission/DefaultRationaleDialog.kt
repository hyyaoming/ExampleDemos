package com.example.sampleview.permission

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.Button
import com.permissionx.guolindev.dialog.RationaleDialog

class DefaultRationaleDialog(
    context: Context,
    private val permissions: List<String>,
    private val message: String,
    private val onConfirm: (() -> Unit)? = null
) : RationaleDialog(context) {

    private lateinit var alertDialog: AlertDialog

    private lateinit var positiveButton: Button
    private lateinit var negativeButton: Button

    init {
        createDialog()
    }

    private fun createDialog() {
        val builder = AlertDialog.Builder(context)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("确定") { _, _ -> onConfirm?.invoke() }
            .setNegativeButton("取消") { dialog, which -> dialog.dismiss() }

        alertDialog = builder.create()
    }

    override fun show() {
        super.show()
        alertDialog.show()
    }

    override fun dismiss() {
        super.dismiss()
        alertDialog.dismiss()
    }

    override fun getPositiveButton(): View {
        return alertDialog.getButton(BUTTON_POSITIVE)
    }

    override fun getNegativeButton(): View {
        return alertDialog.getButton(BUTTON_NEGATIVE)
    }

    override fun getPermissionsToRequest(): List<String> = permissions
}
