package com.example.sampleview.permission

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.sampleview.permission.api.PermissionRequester
import com.example.sampleview.permission.impl.PermissionXBuilder
import com.example.sampleview.permission.model.PermissionHost
import com.example.sampleview.permission.model.PermissionResult
import com.example.sampleview.permission.util.isPermissionsAllGranted
import kotlinx.coroutines.flow.Flow

/**
 * PermissionClient 是权限请求的工厂类和统一入口，负责创建和管理权限请求器（[PermissionRequester]）实例。
 *
 * 该类封装了底层权限请求框架（如 PermissionX）的具体实现细节，提供统一的接口供业务层调用，
 * 使业务层无需关注底层实现，可以灵活替换权限请求框架而不影响调用代码。
 *
 * 通过多种静态方法（from）可以基于 Activity、Fragment 或自定义构建器创建 PermissionClient 实例，
 * 并通过该实例发起权限请求，支持挂起式请求和响应式 Flow 请求两种调用方式。
 *
 * 同时提供了工具方法用于快速判断权限是否已全部授予，方便调用前的权限状态检查。
 *
 * 设计目标：
 * - 解耦业务调用与底层权限框架实现，易于维护和扩展。
 * - 统一管理权限请求流程入口，便于实现默认配置和权限缓存等功能。
 * - 支持多种权限请求调用风格（挂起、Flow）。
 *
 * 使用示例：
 * ```
 * // 基于 Activity 创建客户端
 * val permissionClient = PermissionClient.from(this)
 *
 * // 挂起方式请求权限
 * lifecycleScope.launch {
 *     val result = permissionClient.request(Manifest.permission.CAMERA)
 *     if (result.allGranted) {
 *         // 权限通过
 *     } else {
 *         // 权限被拒绝
 *     }
 * }
 *
 * // Flow 方式请求权限
 * permissionClient.requestFlow(Manifest.permission.CAMERA)
 *     .onEach { result ->
 *         // 处理结果
 *     }.launchIn(lifecycleScope)
 * ```
 */
class PermissionClient private constructor(private val requester: PermissionRequester) {
    /**
     * 挂起式请求权限，适用于协程环境，底层委托给具体权限请求器实现。
     *
     * @param permissions 需要请求的权限列表，变长参数。
     * @return 权限请求结果，包含是否全部授予及详细状态。
     */
    suspend fun request(vararg permissions: String): PermissionResult {
        return requester.request(*permissions)
    }

    /**
     * Flow 方式请求权限，适用于响应式组合编程，底层委托给具体权限请求器实现。
     *
     * @param permissions 需要请求的权限列表，变长参数。
     * @return 发射权限请求结果的 Flow。
     */
    fun requestFlow(vararg permissions: String): Flow<PermissionResult> {
        return requester.requestFlow(*permissions)
    }

    companion object {
        /**
         * 创建绑定于 Activity 的 PermissionClient 实例。
         *
         * @param activity 当前权限请求宿主，通常为 FragmentActivity。
         * @return 绑定 Activity 的 PermissionClient 实例。
         */
        fun from(activity: FragmentActivity): PermissionClient {
            return from(PermissionXBuilder(PermissionHost.ActivityHost(activity)))
        }

        /**
         * 创建绑定于 Fragment 的 PermissionClient 实例。
         *
         * @param fragment 当前权限请求宿主，通常为 Fragment。
         * @return 绑定 Fragment 的 PermissionClient 实例。
         */
        fun from(fragment: Fragment): PermissionClient {
            return from(PermissionXBuilder(PermissionHost.FragmentHost(fragment)))
        }

        /**
         * 使用自定义构建器创建 PermissionClient 实例，支持灵活配置。
         *
         * @param builder 自定义的权限请求构建器，需实现 [PermissionRequester.Builder]。
         * @return 由该构建器构建的 PermissionClient 实例。
         */
        fun from(builder: PermissionRequester.Builder): PermissionClient {
            return PermissionClient(builder.build())
        }

        /**
         * 判断上下文是否已授予所有指定权限。
         *
         * @param context 用于权限检查的上下文，通常为 Activity 或 ApplicationContext。
         * @param permission 变长参数，表示需要检查的权限列表。
         * @return true 表示所有权限均已授予，false 表示至少有一个权限未授予。
         */
        fun hasPermissions(context: Context, vararg permission: String): Boolean {
            return context.isPermissionsAllGranted(permission.toList())
        }
    }
}
