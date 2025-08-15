package com.example.sampleview.permission.api

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.sampleview.permission.model.PermissionResult
import kotlinx.coroutines.flow.Flow

/**
 * 权限请求者接口，定义权限请求的统一入口。
 *
 * 实现类负责具体执行权限请求操作，支持通过
 * [FragmentActivity] 或 [Fragment] 发起权限请求，
 * 并以 [PermissionResult] 返回请求结果，
 * 包含权限是否全部授予、已授予权限列表、被拒绝权限列表等信息。
 */
interface PermissionRequester {

    /**
     * @param permissions 需要请求的权限列表，变长参数形式。
     * @return 权限请求结果 [PermissionResult]，包含授权状态及详细信息。
     */
    suspend fun request(vararg permissions: String): PermissionResult

    /**
     * 以 Flow 方式请求权限，更适合组合式响应编程场景。
     */
    fun requestFlow(vararg permissions: String): Flow<PermissionResult>

    /**
     * 权限请求者构建器，支持灵活配置权限请求流程中的各种行为和回调。
     *
     * 通过 Builder 模式进行构造，便于扩展和替换底层权限请求框架，
     * 以及对权限请求流程中的生命周期回调、解释提示和跳转设置等进行统一管理。
     */
    interface Builder {
        /**
         * 设置权限请求生命周期切片。
         *
         * 生命周期切片用于监听权限请求流程中的各个关键阶段，
         * 例如请求前、请求后等，方便添加业务逻辑或扩展功能。
         *
         * @param slice 权限生命周期切片实例。
         * @return 当前 Builder 实例，支持链式调用。
         */
        fun setLifecycleSlice(slice: PermissionLifecycleSlice): Builder

        /**
         * 设置是否启用请求前向用户解释权限用途的功能。
         *
         * 当启用后，权限请求前会主动展示权限使用原因，
         * 提高权限请求的通过率和用户体验。
         *
         * @param enabled 是否启用请求前解释功能。
         * @return 当前 Builder 实例，支持链式调用。
         */
        fun setExplainReasonBeforeRequest(enabled: Boolean): Builder

        /**
         * 设置权限请求理由的回调接口。
         *
         * 该回调用于自定义权限请求时的理由说明弹窗逻辑，
         * 允许业务展示个性化的说明界面或处理流程。
         *
         * @param callback 权限解释回调接口实例。
         * @return 当前 Builder 实例，支持链式调用。
         */
        fun setExplainCallback(callback: PermissionExplainCallback): Builder

        /**
         * 设置权限被永久拒绝时跳转系统设置页的回调接口。
         *
         * 该回调用于处理用户永久拒绝权限后的引导操作，
         * 例如弹出提示、跳转设置页等自定义行为。
         *
         * @param callback 跳转设置页回调接口实例。
         * @return 当前 Builder 实例，支持链式调用。
         */
        fun setForwardCallback(callback: PermissionForwardCallback): Builder

        /**
         * 设置待请求的权限列表。
         *
         * 此方法用于通过 Builder 模式预设权限请求的目标权限，适用于链式调用风格。
         *
         * @param permissions 需要申请的权限，例如 [android.Manifest.permission.CAMERA] 等。
         * @return 当前 Builder 实例，可用于链式调用。
         */
        fun setRequestPermissions(vararg permissions: String): Builder

        /**
         * 构建并返回配置完成的权限请求者实例。
         *
         * @return 构建好的 [PermissionRequester] 实例。
         */
        fun build(): PermissionRequester
    }
}
