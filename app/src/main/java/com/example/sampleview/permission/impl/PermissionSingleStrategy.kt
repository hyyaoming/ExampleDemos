package com.example.sampleview.permission.impl

import android.Manifest
import com.example.sampleview.R
import com.example.sampleview.permission.model.PermissionDesc
import com.example.sampleview.permission.api.PermissionDescStrategy

/**
 * 单个权限描述策略，用于匹配单一权限的描述信息。
 */
class PermissionSingleStrategy : PermissionDescStrategy {

    // 单个权限对应的描述映射表
    private val map = mapOf(
        Manifest.permission.RECORD_AUDIO to PermissionDesc(R.mipmap.ic_camera, "相机权限", "用于拍照上传凭证等")
    )

    /**
     * 尝试匹配传入权限集合中的第一个权限，若匹配则返回对应的权限描述。
     *
     * @param permissions 当前剩余待匹配的权限集合
     * @return 匹配到的权限描述，未匹配则返回 null
     */
    override fun match(permissions: Set<String>): PermissionDesc? {
        return permissions.firstOrNull()?.let { map[it] }
    }

    /**
     * 返回本策略处理的所有权限集合，用于外部去重。
     */
    override fun getHandledPermissions(): Set<String> {
        return map.keys
    }
}
