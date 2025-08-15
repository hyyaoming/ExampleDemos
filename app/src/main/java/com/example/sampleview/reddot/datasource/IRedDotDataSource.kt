package com.example.sampleview.reddot.datasource

import com.example.sampleview.reddot.core.RedDotData
import com.example.sampleview.reddot.core.RedDotKey

/**
 * 红点数据源接口，用于定义红点数据的获取与上报行为。
 * 实现类应负责从远程或本地加载红点状态数据，并支持将“已读”状态上报。
 */
interface IRedDotDataSource {

    /**
     * 异步获取红点数据列表。
     *
     * @return 包含所有需要展示红点的 [RedDotData] 列表。
     * 实现应根据业务逻辑从网络、本地缓存或其他来源获取最新数据。
     */
    suspend fun fetch(): List<RedDotData>

    /**
     * 异步上报红点的已读状态。
     *
     * @param keys 要上报为“已读”的红点键列表。
     * @param extraInfo 可选的附加信息，用于上报时附带上下文（例如设备信息、用户行为等）。
     * @return 返回成功上报的红点键列表（通常是 [keys] 的子集，表示成功上报的部分）。
     */
    suspend fun reportRead(
        keys: List<RedDotKey>,
        extraInfo: Map<String, Any>? = null,
    ): List<RedDotKey>
}
