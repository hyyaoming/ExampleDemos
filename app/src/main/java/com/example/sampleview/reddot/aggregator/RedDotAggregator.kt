package com.example.sampleview.reddot.aggregator

import com.example.sampleview.reddot.core.RedDotData
import com.example.sampleview.reddot.core.RedDotKey

/**
 * 红点聚合器接口，用于定义如何根据特定规则对多个红点数据进行聚合处理。
 *
 * 实现类可根据 [matcher] 提供的匹配规则，从传入的红点数据中筛选目标项，并通过 [aggregate]
 * 方法自定义聚合逻辑（如是否存在未读红点、合并红点数量、生成聚合标记等）。
 *
 * @param T 聚合结果的类型，可根据业务需求自定义（例如 Boolean 表示是否有未读红点，Int 表示数量等）。
 */
interface RedDotAggregator<T> {

    /**
     * 聚合器的路径匹配器，用于筛选要参与聚合的红点数据。
     * 只有 key 匹配此 matcher 的红点才会被传入 [aggregate] 方法进行处理。
     */
    val matcher: AggregationMatcher

    /**
     * 执行红点聚合操作。
     *
     * @param redDots 当前所有红点的键值对集合。
     * 键为 [RedDotKey]，值为其对应的 [RedDotData]。
     * 实现类应根据 [matcher] 提取相关红点项，并聚合生成最终结果。
     *
     * @return 聚合结果，类型为泛型参数 [T]。
     */
    suspend fun aggregate(redDots: Map<RedDotKey, RedDotData>): T

    /**
     * 定义一个路径匹配器接口，用于判断某个字符串路径是否符合某种聚合匹配规则。
     *
     * 通常用于红点系统中某些路径（key）是否应被聚合判断为“有红点”，
     * 比如用户层级聚合、模块层级聚合等。
     */
    interface AggregationMatcher {

        /**
         * 判断传入的路径是否匹配当前匹配器的规则。
         *
         * @param path 要判断的完整路径，例如 "profile/user/123/unRead"
         * @return 如果该路径符合匹配规则，则返回 true；否则返回 false。
         */
        fun matches(path: String): Boolean

        /**
         * 组合另一个匹配器，返回一个新的匹配器，只有当两个匹配器都匹配时才返回 true。
         *
         * 常用于需要多个条件同时成立的场景。
         *
         * @param other 另一个匹配器。
         * @return 新的匹配器，表示两个匹配器的 AND（与）逻辑组合。
         */
        fun and(other: AggregationMatcher) = object : AggregationMatcher {
            override fun matches(path: String): Boolean =
                this@AggregationMatcher.matches(path) && other.matches(path)
        }

        /**
         * 组合另一个匹配器，返回一个新的匹配器，只要任一匹配器匹配就返回 true。
         *
         * 常用于只需要满足任意条件的场景。
         *
         * @param other 另一个匹配器。
         * @return 新的匹配器，表示两个匹配器的 OR（或）逻辑组合。
         */
        fun or(other: AggregationMatcher) = object : AggregationMatcher {
            override fun matches(path: String): Boolean =
                this@AggregationMatcher.matches(path) || other.matches(path)
        }
    }
}
