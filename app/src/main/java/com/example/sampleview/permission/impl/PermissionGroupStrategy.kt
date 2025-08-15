package com.example.sampleview.permission.impl

import android.Manifest
import com.example.sampleview.R
import com.example.sampleview.permission.model.PermissionDesc
import com.example.sampleview.permission.api.PermissionDescStrategy

/**
 * 权限组匹配策略
 *
 * 用于识别一组权限是否匹配某个权限组，并返回对应的权限描述。
 */
class PermissionGroupStrategy : PermissionDescStrategy {

    // 权限组与对应的权限描述映射
    private val groupMap = mapOf(
        setOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION) to PermissionDesc(
            R.mipmap.ic_location,
            "定位权限",
            "用于获取附近信息"
        )
        // 可扩展更多权限组合及其描述
    )

    /**
     * 如果传入的权限集合中包含某个完整的权限组，则返回该组对应的 PermissionDesc，否则返回 null。
     *
     * @param permissions 当前请求的权限集合
     * @return 匹配的权限描述或 null
     */
    override fun match(permissions: Set<String>): PermissionDesc? {
        return groupMap.entries.firstOrNull { permissions.containsAll(it.key) }?.value
    }

    /**
     * 返回该策略关联的所有权限集合，用于去重和匹配优化。
     *
     * @return 所有关联权限集合
     */
    override fun getHandledPermissions(): Set<String> {
        return groupMap.keys.flatten().toSet()
    }
}
