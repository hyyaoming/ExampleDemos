package com.example.sampleview.permission.impl

import com.example.sampleview.permission.api.PermissionExplainCallback
import com.example.sampleview.permission.api.PermissionForwardCallback
import com.example.sampleview.permission.api.PermissionLifecycleSlice
import com.example.sampleview.permission.api.PermissionRequester
import com.example.sampleview.permission.model.PermissionHost

/**
 * BaseBuilder 是权限请求构建器的抽象基类，封装了权限请求过程中常用的配置参数及链式设置方法。
 *
 * 该类实现了 [PermissionRequester.Builder] 接口的公共方法，
 * 用于统一管理权限请求生命周期切片、权限解释回调、跳转设置页回调及请求权限列表等配置项。
 *
 * 子类只需继承此类，专注于实现具体的 [build] 方法来创建对应的权限请求器实例，
 * 便于多种权限请求框架实现之间复用公共配置逻辑，提高代码复用性和可维护性。
 *
 * @property host 权限请求宿主的封装类型，通常为 Activity 或 Fragment 的包装。
 */
abstract class BaseBuilder(protected val host: PermissionHost) : PermissionRequester.Builder {
    /**
     * 权限请求生命周期切片，支持监听请求前后等关键生命周期事件。
     */
    protected var lifecycleSlice: PermissionLifecycleSlice? = null

    /**
     * 是否在请求权限前展示权限用途说明对话框。
     */
    protected var shouldExplainBeforeRequest = false

    /**
     * 权限请求理由的自定义回调，用于展示个性化说明界面。
     */
    protected var explainReasonCallback: PermissionExplainCallback? = null

    /**
     * 当权限被永久拒绝时跳转系统设置页的回调。
     */
    protected var forwardSettingCallback: PermissionForwardCallback? = null

    /**
     * 待请求的权限列表，支持多个权限同时申请。
     */
    protected var requestPermissions: List<String> = emptyList()

    /**
     * 设置待请求的权限列表。
     *
     * 支持可变参数形式，方便调用者同时申请多个权限。
     * 支持链式调用，方便在 Builder 模式中连续配置多个参数。
     *
     * @param permissions 需要申请的权限字符串，例如 [android.Manifest.permission.CAMERA]。
     * @return 当前 Builder 实例，用于链式调用。
     */
    override fun setRequestPermissions(vararg permissions: String) = apply {
        this.requestPermissions = permissions.toList()
    }

    /**
     * 设置权限请求生命周期切片。
     *
     * 生命周期切片用于监听权限请求流程中的关键时刻，方便添加业务扩展逻辑。
     *
     * @param slice 生命周期切片实例。
     * @return 当前 Builder 实例，用于链式调用。
     */
    override fun setLifecycleSlice(slice: PermissionLifecycleSlice) = apply {
        this.lifecycleSlice = slice
    }

    /**
     * 设置是否在请求权限前先展示权限用途说明对话框。
     *
     * 开启后权限请求前会弹出理由说明，提升用户授权意愿。
     *
     * @param enabled 是否启用请求前说明功能。
     * @return 当前 Builder 实例，用于链式调用。
     */
    override fun setExplainReasonBeforeRequest(enabled: Boolean) = apply {
        this.shouldExplainBeforeRequest = enabled
    }

    /**
     * 设置权限请求理由的自定义回调接口。
     *
     * 用于业务自定义权限解释逻辑，例如弹窗提示权限用途等。
     *
     * @param callback 权限解释回调接口实例。
     * @return 当前 Builder 实例，用于链式调用。
     */
    override fun setExplainCallback(callback: PermissionExplainCallback) = apply {
        this.explainReasonCallback = callback
    }

    /**
     * 设置当权限被永久拒绝时，引导用户跳转系统设置页的回调接口。
     *
     * 便于实现自定义的跳转提示或引导流程。
     *
     * @param callback 跳转设置页回调接口实例。
     * @return 当前 Builder 实例，用于链式调用。
     */
    override fun setForwardCallback(callback: PermissionForwardCallback) = apply {
        this.forwardSettingCallback = callback
    }
}
