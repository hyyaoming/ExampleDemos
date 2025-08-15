package com.example.sampleview.permission.api

import com.example.sampleview.permission.model.PermissionDesc

/**
 * 权限描述匹配策略接口，定义权限描述的匹配逻辑及关联权限集合。
 *
 * 实现该接口的策略用于判断输入的权限集合中，是否匹配到特定的权限描述，
 * 并提供当前策略负责的所有权限集合，方便外部进行权限去重处理。
 */
interface PermissionDescStrategy {

    /**
     * 匹配权限描述。
     *
     * 根据传入的剩余未匹配权限集合，判断是否能匹配到对应的权限描述。
     * 若匹配成功，返回对应的 [PermissionDesc]；否则返回 null。
     *
     * @param remainingPermissions 当前待匹配的权限集合（尚未被其他策略处理）
     * @return 匹配到的权限描述对象，或 null 表示无匹配
     */
    fun match(remainingPermissions: Set<String>): PermissionDesc?

    /**
     * 获取该策略关联的所有权限集合。
     *
     * 用于标识该策略负责匹配的权限范围，外部可根据此集合对权限进行去重，
     * 避免不同策略间重复匹配相同权限。
     *
     * @return 当前策略管理的权限集合
     */
    fun getHandledPermissions(): Set<String>
}
