package com.example.sampleview.permission.model

/**
 * 权限说明信息结构体。
 *
 * 用于描述单个权限的图标、标题和详细说明内容，
 * 可用于权限弹窗、列表展示等场景。
 *
 * @property iconResId 权限对应的图标资源 ID
 * @property title 权限的名称或简要说明（如“位置权限”）
 * @property detail 权限的详细用途或申请理由（如“用于获取您的当前位置以推荐附近服务”）
 */
data class PermissionDesc(
    val iconResId: Int,
    val title: String,
    val detail: String
)
