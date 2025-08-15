package com.example.sampleview.permission.api

import com.example.sampleview.permission.model.PermissionDesc

/**
 * 解析权限描述的接口，用于将一组权限转换为对应的权限描述信息列表。
 *
 * 该接口负责根据权限字符串列表，返回用户友好的权限图标、标题和详细描述，
 * 以便在权限请求界面或者提示对话框中展示给用户。
 */
interface PermissionDescriptionResolver {
    /**
     * 根据传入的权限列表，解析并返回对应的权限描述集合。
     *
     * @param permissions 需要解析的权限列表，通常是 Android 权限字符串列表，如 Manifest.permission.CAMERA。
     * @return 返回与权限对应的描述信息列表，每个描述包含图标资源、标题和详细说明。
     *         该列表可以包含多个描述，尤其当权限列表中包含多个权限或权限组时。
     */
    fun resolve(permissions: List<String>): ArrayList<PermissionDesc>
}
