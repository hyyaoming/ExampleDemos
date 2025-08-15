package com.example.sampleview.reddot.core

/**
 * 红点标识唯一键，使用路径风格的字符串表示红点节点位置。
 *
 * 例如：
 * - "profile/unread"
 * - "home/messages/unreadCount"
 *
 * 该类使用 Kotlin 的 @JvmInline value class 优化为包装类型，运行时不增加额外开销。
 *
 * @property fullKey 完整的路径字符串，多个路径段以 '/' 分隔
 */
@JvmInline
value class RedDotKey private constructor(val fullKey: String) {

    /**
     * 按照 '/' 分隔符拆分 fullKey，得到路径段列表。
     *
     * 例如：fullKey = "profile/user/123"
     * 返回 ["profile", "user", "123"]
     */
    val segments: List<String> get() = fullKey.split("/")

    /**
     * 获取模块名称，即路径的第一级路径段。
     *
     * 例如 "profile/user/123" 的 moduleName 为 "profile"
     * 如果路径为空，则返回空字符串。
     */
    val moduleName: String get() = segments.firstOrNull().orEmpty()

    /**
     * 获取当前节点的父节点 Key。
     *
     * 例如：
     * fullKey = "profile/user/123"
     * parent 返回 RedDotKey("profile/user")
     *
     * 如果当前节点已经是顶层节点（只有一个路径段），则返回 null。
     */
    val parent: RedDotKey?
        get() = segments.dropLast(1) // 去除最后一级路径段
            .takeIf { it.isNotEmpty() } // 确保剩余路径非空
            ?.let { of(*it.toTypedArray()) } // 重新构造 RedDotKey

    /**
     * 创建当前 Key 的子节点。
     *
     * 会在当前 fullKey 后追加 '/' 和新的路径段 segment，生成新的完整路径。
     *
     * 例如：
     * 当前 Key: "profile/user"
     * 调用 child("123") 后，返回 Key: "profile/user/123"
     *
     * @param segment 子路径段，不能为空且不能包含 '/'，否则抛出异常。
     * @return 新生成的子节点 RedDotKey
     * @throws IllegalArgumentException 如果 segment 为空或包含 '/'
     */
    fun child(segment: String): RedDotKey {
        require(segment.isNotBlank()) { "Segment must not be blank" }
        require(!segment.contains("/")) { "Segment must not contain '/'" }
        return RedDotKey("$fullKey/$segment")
    }

    /**
     * 返回当前 Key 的完整路径字符串表示。
     *
     * 等同于 fullKey 字段。
     */
    override fun toString() = fullKey

    companion object {

        /**
         * 通过路径段构造 RedDotKey。
         *
         * 传入的路径段必须非空，且每个段都不能为空字符串或包含 '/'。
         *
         * @param segments 路径段数组，例如 ["profile", "user", "123"]
         * @return 构造的 RedDotKey 实例
         * @throws IllegalArgumentException 如果路径段数组为空，或包含非法路径段
         */
        fun of(vararg segments: String): RedDotKey {
            require(segments.isNotEmpty()) { "Key path cannot be empty" }
            segments.forEach {
                require(it.isNotBlank()) { "Path segment must not be blank" }
                require(!it.contains("/")) { "Path segment must not contain '/'" }
            }
            return RedDotKey(segments.joinToString("/"))
        }

        /**
         * 通过完整路径字符串构造 RedDotKey，自动拆分路径段并过滤空段。
         *
         * 例如：
         * 输入 "profile//user/123" 会被拆分为 ["profile", "user", "123"]
         *
         * @param key 以 '/' 分割的完整路径字符串
         * @return 构造的 RedDotKey 实例
         */
        fun fromString(key: String): RedDotKey {
            val segments = key.split("/")
                .filter { it.isNotBlank() }
            return of(*segments.toTypedArray())
        }
    }
}