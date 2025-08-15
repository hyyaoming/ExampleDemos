package com.example.sampleview.permission.model

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

/**
 * 权限请求宿主的封装，用于统一管理请求权限时所需的上下文来源。
 *
 * 使用密封类表示两种宿主类型：
 * - ActivityHost：基于 FragmentActivity 的权限请求宿主。
 * - FragmentHost：基于 Fragment 的权限请求宿主。
 *
 * 通过 [context] 属性获取统一的 Context 对象，方便权限请求调用时使用。
 */
sealed class PermissionHost {

    /**
     * 基于 FragmentActivity 的权限请求宿主封装。
     *
     * @param activity 当前的 FragmentActivity 实例，作为权限请求的上下文。
     */
    data class ActivityHost(val activity: FragmentActivity) : PermissionHost()

    /**
     * 基于 Fragment 的权限请求宿主封装。
     *
     * @param fragment 当前的 Fragment 实例，作为权限请求的上下文。
     */
    data class FragmentHost(val fragment: Fragment) : PermissionHost()

    /**
     * 获取权限请求所需的 Context 对象。
     *
     * 当宿主为 ActivityHost 时返回对应的 Activity；
     * 当宿主为 FragmentHost 时返回 Fragment 绑定的 Context。
     */
    val context: Context
        get() = when (this) {
            is ActivityHost -> activity
            is FragmentHost -> fragment.requireContext()
        }
}
