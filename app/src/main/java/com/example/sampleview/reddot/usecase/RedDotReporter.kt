package com.xnhz.libbase.reddot.usecase

import com.example.sampleview.reddot.core.RedDotKey
import com.example.sampleview.reddot.datasource.RedDotSourceRegistry

/**
 * 红点状态上报器，负责将“已读”状态反馈给对应模块的数据源。
 *
 * 通过分组的方式按模块逐个调用对应的数据源上报接口，
 * 支持批量上报并合并返回成功上报的红点 Key 列表。
 *
 * @property registry 红点数据源注册中心，管理模块对应的数据源
 */
class RedDotReporter(private val registry: RedDotSourceRegistry) {
    /**
     * 上报指定红点 Key 列表的已读状态。
     *
     * 根据红点 Key 的模块名进行分组，针对每个模块调用对应的数据源的上报接口，
     * 支持传入额外的上下文信息（例如设备、用户行为等）。
     *
     * @param keys 需要上报为“已读”状态的红点 Key 列表
     * @param extraInfo 可选的附加信息，用于上报时携带额外上下文
     * @return 返回所有模块成功上报的红点 Key 合并列表
     */
    suspend fun reportRead(
        keys: List<RedDotKey>,
        extraInfo: Map<String, Any>? = null,
    ): List<RedDotKey> {
        return keys
            .groupBy { it.moduleName }
            .entries
            .flatMap { (module, keysGroup) ->
                val source = registry.getSource(module)
                runCatching {
                    source?.reportRead(keysGroup, extraInfo)
                }.getOrNull().orEmpty()
            }
    }
}
