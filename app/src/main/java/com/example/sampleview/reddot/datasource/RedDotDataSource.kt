package com.example.sampleview.reddot.datasource

import com.example.sampleview.reddot.core.RedDotData
import com.example.sampleview.reddot.core.RedDotKey

/**
 * 红点数据源接口，用于从远程或本地源获取原始数据并适配为通用的 [RedDotData] 格式。
 *
 * 此接口继承自 [IRedDotDataSource]，通过组合适配器模式，将原始数据转换为框架统一处理的数据结构。
 *
 * @param Raw 表示数据源中返回的原始数据类型。
 */
interface RedDotDataSource<Raw> : IRedDotDataSource {

    /**
     * 异步获取原始红点数据（未适配前）。
     *
     * 由具体实现类根据业务来源（如网络、数据库、缓存等）返回原始数据。
     */
    suspend fun fetchRaw(): Raw

    /**
     * 将原始数据适配为标准的红点数据 [RedDotData] 的适配器。
     */
    val adapter: RedDotAdapter<Raw>

    /**
     * 异步获取已适配后的红点数据列表。
     *
     * 默认调用 [fetchRaw] 并使用 [adapter] 进行数据转换。
     */
    override suspend fun fetch(): List<RedDotData> = adapter.adapt(fetchRaw())

    /**
     * 上报已读红点项，可携带附加信息。
     *
     * 默认调用 [adapter] 的上报方法。
     *
     * @param keys 已读的红点 key 列表。
     * @param extraInfo 上报时附带的额外参数信息。
     * @return 返回已成功上报的红点 key 列表。
     */
    override suspend fun reportRead(
        keys: List<RedDotKey>,
        extraInfo: Map<String, Any>?,
    ): List<RedDotKey> = adapter.reportRead(keys, extraInfo)

    /**
     * 红点数据适配器接口，将原始数据转为通用 [RedDotData]，并处理上报逻辑。
     *
     * @param Raw 原始红点数据的类型。
     */
    interface RedDotAdapter<Raw> {

        /**
         * 将原始红点数据转换为 [RedDotData] 列表。
         *
         * @param raw 原始红点数据。
         * @return 转换后的红点数据列表。
         */
        suspend fun adapt(raw: Raw): List<RedDotData>

        /**
         * 上报红点已读事件，可带附加信息。
         *
         * @param keys 已读的红点 key 列表。
         * @param extraInfo 附加的上报参数信息。
         * @return 成功上报的红点 key 列表。
         */
        suspend fun reportRead(keys: List<RedDotKey>, extraInfo: Map<String, Any>?): List<RedDotKey>
    }
}

