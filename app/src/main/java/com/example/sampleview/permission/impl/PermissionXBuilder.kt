package com.example.sampleview.permission.impl

import com.example.sampleview.permission.api.PermissionRequester
import com.example.sampleview.permission.model.PermissionHost

/**
 * PermissionXBuilder 是基于 [BaseBuilder] 的具体权限请求构建器实现类。
 *
 * 它负责使用基类中已配置的权限请求参数，构建出具体的 [PermissionRequester] 实例，
 * 该实例基于 PermissionX 框架实现具体权限请求流程。
 *
 * 通过继承 [BaseBuilder]，该类复用通用的配置方法与属性，简化了权限请求器的构建逻辑。
 *
 * @param host 权限请求的宿主封装，一般为 Activity 或 Fragment 的包装类型 [PermissionHost]。
 */
class PermissionXBuilder(host: PermissionHost) : BaseBuilder(host) {

    /**
     * 构建并返回基于 PermissionX 实现的权限请求器实例。
     *
     * 使用构建器中设置的权限列表、生命周期切片、解释理由回调等参数初始化请求器，
     * 以实现完整的权限请求逻辑。
     *
     * @return 创建好的 [PermissionRequester] 实例，用于发起权限请求。
     */
    override fun build(): PermissionRequester {
        return PermissionXRequesterImpl(
            host = host,
            requestPermissions = requestPermissions,
            lifecycleSlice = lifecycleSlice,
            shouldExplainBeforeRequest = shouldExplainBeforeRequest,
            explainReasonCallback = explainReasonCallback,
            forwardSettingCallback = forwardSettingCallback
        )
    }
}
