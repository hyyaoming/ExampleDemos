package com.example.sampleview.permission.impl

import com.example.sampleview.permission.api.PermissionDescStrategy
import com.example.sampleview.permission.api.PermissionDescriptionResolver
import com.example.sampleview.permission.model.PermissionDesc

/**
 * 默认的权限描述提供者，实现了 [PermissionDescriptionResolver] 接口。
 *
 * 通过组合多种权限匹配策略（单个权限策略和权限组策略）来解析权限描述，
 * 能够支持同时处理单个权限和权限组的描述映射，满足复杂权限请求的展示需求。
 */
class DefaultPermissionDescProvider : PermissionDescriptionResolver {

    // 持有的策略列表，依次尝试匹配权限描述
    private val strategies = ArrayList<PermissionDescStrategy>()

    init {
        // 添加单个权限匹配策略
        strategies.add(PermissionSingleStrategy())
        // 添加权限组匹配策略
        strategies.add(PermissionGroupStrategy())
    }

    /**
     * 根据传入的权限列表，依次使用策略匹配，返回对应的权限描述列表。
     *
     * 1. 将权限列表转为集合方便差集计算；
     * 2. 通过每个策略匹配未被处理的权限集合，匹配成功则添加对应描述；
     * 3. 通过策略获取已处理的权限，避免重复匹配。
     *
     * @param permissions 请求的权限列表
     * @return 返回权限描述列表，包含图标、标题及描述信息
     */
    override fun resolve(permissions: List<String>): ArrayList<PermissionDesc> {
        val permissionsSet = permissions.toSet()
        val result = ArrayList<PermissionDesc>()
        val matchedPermissions = mutableSetOf<String>()

        for (strategy in strategies) {
            val remaining = permissionsSet - matchedPermissions
            val desc = strategy.match(remaining)
            if (desc != null) {
                // 获取本策略已处理的权限，避免重复匹配
                matchedPermissions.addAll(strategy.getHandledPermissions().intersect(remaining))
                result.add(desc)
            }
        }
        return result
    }
}
