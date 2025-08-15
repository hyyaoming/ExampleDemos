package com.example.sampleview.permission.impl

import android.content.Context
import com.example.sampleview.permission.api.PermissionExplainCallback
import com.example.sampleview.permission.api.PermissionForwardCallback
import com.example.sampleview.permission.api.PermissionLifecycleSlice
import com.example.sampleview.permission.api.PermissionRequester
import com.example.sampleview.permission.model.PermissionHost
import com.example.sampleview.permission.model.PermissionResult
import com.example.sampleview.permission.util.isPermissionsAllGranted
import com.example.sampleview.permission.util.safeCall
import com.example.sampleview.permission.util.safeResumeWith
import com.permissionx.guolindev.PermissionX
import com.permissionx.guolindev.request.PermissionBuilder
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * 使用 PermissionX 框架封装的权限请求实现类，支持以更灵活和统一的方式进行权限管理。
 *
 * 此类适配 Activity 或 Fragment 作为权限请求宿主，支持请求生命周期的扩展、权限用途说明、
 * 以及权限被永久拒绝后的跳转设置页提示等定制行为，适用于大部分 Android 权限场景。
 *
 * ## 特性：
 * - 支持传入 Fragment 或 Activity 上下文（通过 [PermissionHost] 封装）；
 * - 支持请求前/请求后生命周期扩展逻辑（如埋点、弹窗等）；
 * - 支持请求前展示权限用途说明；
 * - 支持权限永久拒绝后跳转设置页提示；
 * - 可结合 Flow 使用，实现响应式权限请求。
 *
 * @param host 权限请求的宿主对象，封装了 Activity 或 Fragment 的引用，必须非空。
 * @param requestPermissions 待请求的权限列表。
 * @param lifecycleSlice 请求生命周期切片，可扩展请求前后行为，如日志、埋点、弹窗等，可选。
 * @param shouldExplainBeforeRequest 是否启用请求前权限用途说明逻辑，默认关闭。
 * @param explainReasonCallback 权限说明回调，展示为何需要权限，可通过弹窗等形式引导用户授权。
 * @param forwardSettingCallback 权限永久拒绝后的跳转设置页回调，用于引导用户前往系统设置页面开启权限。
 */
internal class PermissionXRequesterImpl internal constructor(
    private val host: PermissionHost,
    private var requestPermissions: List<String> = emptyList(),
    private val lifecycleSlice: PermissionLifecycleSlice? = null,
    private val shouldExplainBeforeRequest: Boolean = false,
    private val explainReasonCallback: PermissionExplainCallback? = null,
    private val forwardSettingCallback: PermissionForwardCallback? = null,
) : PermissionRequester {

    /**
     * 以挂起函数方式请求权限，适用于协程上下文中直接调用。
     *
     * @param permissions 要请求的权限，可传多个，例如 [android.Manifest.permission.CAMERA]。
     * @return [PermissionResult] 包含每个权限的授权状态，以及是否全部授权（`allGranted`）。
     *
     * 使用示例：
     * ```
     * val result = permissionRequester.request(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
     * if (result.allGranted) {
     *     // 权限已全部授予
     * } else {
     *     // 有权限被拒绝，具体可查看 result.deniedPermissions
     * }
     * ```
     */
    override suspend fun request(vararg permissions: String): PermissionResult {
        return requestInternal(*permissions)
    }

    /**
     * 以 Kotlin Flow 的形式请求权限，适用于响应式场景。
     *
     * @param permissions 需要请求的权限列表。
     * @return [Flow] 对象，发射一次权限请求结果 [PermissionResult]。
     *
     * 使用场景示例：
     * ```
     * permissionRequester.requestFlow(Manifest.permission.CAMERA)
     *     .onEach { result ->
     *         if (result.allGranted) {
     *             // 权限全部授予
     *         } else {
     *             // 有权限被拒绝
     *         }
     *     }.launchIn(lifecycleScope)
     * ```
     */
    override fun requestFlow(vararg permissions: String): Flow<PermissionResult> = flow {
        emit(requestInternal(*permissions))
    }

    /**
     * 权限请求的内部实现方法，支持使用 Activity 或 Fragment 作为请求上下文。
     *
     * 请求流程详解：
     * 1. 尝试安全获取有效 Context，若无法获取则直接返回权限失败结果。
     * 2. 检查所有请求权限是否已经全部授予，若已授予则立即返回成功结果。
     * 3. 调用权限请求生命周期切片的 onBeforeRequest 钩子，允许在请求前进行拦截或预处理。
     * 4. 使用 PermissionX 初始化权限请求构建器，捕获异常防止崩溃。
     * 5. 根据配置，设置权限请求理由解释（Explain）及跳转设置页（Forward）回调，支持自定义 UI 和流程拦截。
     * 6. 发起权限请求，等待异步回调结果。
     * 7. 调用权限请求生命周期切片的 onAfterRequest 钩子，允许请求后处理。
     * 8. 通过协程恢复调用者，返回最终的权限请求结果。
     *
     * @param permissions 请求的权限列表，可变参数。
     * @return 返回权限请求结果 [PermissionResult]，包含所有权限的授权状态信息。
     */
    private suspend fun requestInternal(vararg permissions: String) = suspendCancellableCoroutine { cont ->
        if (!permissions.isEmpty()) {
            this.requestPermissions = permissions.toList()
        }
        // Step 1: 尝试获取有效上下文
        val context = getContextOrReturn(cont, requestPermissions)
        if (context == null) return@suspendCancellableCoroutine

        // Step 2: 若权限已全部授予，立即返回成功
        if (isAllGranted(context, requestPermissions, cont)) return@suspendCancellableCoroutine

        // Step 3: 回调生命周期前置钩子
        val requestList = requestPermissions.filter { !PermissionX.isGranted(context, it) }
        safeCall({ lifecycleSlice?.onBeforeRequest(requestList, context) }, Unit)

        // Step 4: 初始化 PermissionX 构建器
        val builder = buildPermissionBuilder(requestPermissions, cont)
        if (builder == null) return@suspendCancellableCoroutine

        // Step 5: 配置权限说明 / 跳转设置回调
        applyExplainAndForwardCallbacks(builder, context)

        // Step 6: 发起权限请求并处理结果
        builder.request { allGranted, grantedList, deniedList ->

            val result = PermissionResult(allGranted = allGranted, granted = grantedList, denied = deniedList)

            // Step 7: 回调生命周期后置钩子
            safeCall({ lifecycleSlice?.onAfterRequest(result, context) }, Unit)

            // Step 8: 恢复协程，返回结果
            cont.safeResumeWith(result)
        }
    }


    /**
     * 获取上下文对象，如果获取失败则直接返回权限失败结果并恢复协程。
     *
     * @param cont 协程续体，用于恢复外部协程。
     * @param permissionList 当前请求的权限列表，用于构造失败结果。
     * @return 有效的 [Context] 实例，若无法获取则返回 null。
     */
    private fun getContextOrReturn(
        cont: CancellableContinuation<PermissionResult>, permissionList: List<String>
    ): Context? {
        val context = runCatching { host.context }.getOrNull()
        if (context == null) {
            cont.safeResumeWith(PermissionResult.allDenied(permissionList))
        }
        return context
    }

    /**
     * 判断指定权限是否全部已授权，若是则直接返回成功结果并恢复协程。
     *
     * @param context 当前有效上下文。
     * @param permissions 权限列表。
     * @param cont 协程续体，用于恢复外部协程。
     * @return 若权限全部已授权则返回 true，表示后续流程无需继续；否则返回 false。
     */
    private fun isAllGranted(
        context: Context, permissions: List<String>, cont: CancellableContinuation<PermissionResult>
    ): Boolean {
        return if (context.isPermissionsAllGranted(permissions)) {
            cont.safeResumeWith(PermissionResult.allGranted(permissions))
            true
        } else false
    }

    /**
     * 构建用于发起权限请求的 PermissionX 构建器实例，支持 Activity 与 Fragment 上下文。
     *
     * @param permissions 请求的权限参数。
     * @param cont 协程续体，用于在发生异常时返回失败结果。
     * @return 构建成功的 [PermissionBuilder] 实例；若构建失败则返回 null。
     */
    private fun buildPermissionBuilder(
        permissions: List<String>, cont: CancellableContinuation<PermissionResult>
    ): PermissionBuilder? {
        return try {
            when (host) {
                is PermissionHost.ActivityHost -> PermissionX.init(host.activity).permissions(permissions)
                is PermissionHost.FragmentHost -> PermissionX.init(host.fragment).permissions(permissions)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cont.safeResumeWith(PermissionResult.allDenied(permissions))
            null
        }
    }

    /**
     * 为权限请求构建器配置解释权限用途的回调和跳转设置页的回调。
     *
     * 支持可选的“请求前说明”配置，以及自定义 UI 的回调封装。
     *
     * @param builder 已初始化的 [PermissionBuilder] 实例。
     * @param context 当前上下文，用于在回调中传入业务处理逻辑。
     */
    private fun applyExplainAndForwardCallbacks(
        builder: PermissionBuilder, context: Context
    ) {
        // 启用请求前权限用途说明
        if (shouldExplainBeforeRequest) builder.explainReasonBeforeRequest()

        // 配置权限用途说明回调
        explainReasonCallback?.let { callback ->
            builder.onExplainRequestReason { scope, deniedList, _ ->
                val wrapper = PermissionXExplainScopeWrapper(scope)
                callback.onExplainRequestReason(deniedList, context, wrapper)
            }
        }

        // 配置权限永久拒绝后的跳转设置回调
        forwardSettingCallback?.let { callback ->
            builder.onForwardToSettings { scope, deniedList ->
                val wrapper = PermissionXForwardScopeWrapper(scope)
                callback.onForwardToSettings(deniedList, context, wrapper)
            }
        }
    }
}
