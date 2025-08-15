package com.example.sampleview.reddot.datasource

import java.util.concurrent.ConcurrentHashMap

/**
 * 红点数据源注册中心，负责管理各模块对应的红点数据源实例。
 *
 * 通过模块名字符串维护数据源的注册与注销，
 * 提供按模块获取单个数据源或获取全部数据源的功能。
 */
class RedDotSourceRegistry {

    /** 线程安全的模块名到数据源映射 */
    private val sources = ConcurrentHashMap<String, IRedDotDataSource>()

    /**
     * 注册某模块对应的红点数据源。
     * 如果模块已存在数据源，则替换为新数据源。
     *
     * @param module 模块名，唯一标识
     * @param source 实现了 IRedDotDataSource 的数据源实例
     */
    fun register(module: String, source: IRedDotDataSource) {
        sources[module] = source
    }

    /**
     * 注销某模块的红点数据源，移除对应映射。
     *
     * @param module 模块名，指定要注销的数据源所属模块
     */
    fun unregister(module: String) {
        sources.remove(module)
    }

    /**
     * 根据模块名获取对应的红点数据源实例。
     *
     * @param module 模块名
     * @return 对应的红点数据源，若未注册则返回 null
     */
    fun getSource(module: String): IRedDotDataSource? = sources[module]

    /**
     * 获取当前所有已注册的红点数据源集合。
     *
     * @return 数据源集合，包含所有模块注册的红点数据源实例
     */
    fun allSources(): Collection<IRedDotDataSource> = sources.values

    /**
     * 清空所有已注册的数据源，注销全部模块对应的红点数据源。
     */
    fun clear() {
        sources.clear()
    }
}
