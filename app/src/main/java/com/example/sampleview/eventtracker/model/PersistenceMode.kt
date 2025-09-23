package com.example.sampleview.eventtracker.model

/**
 * 事件持久化模式，用于控制事件在内存与持久化存储（DB/文件）中的处理策略。
 */
enum class PersistenceMode {

    /**
     * BEST_EFFORT（总是持久化）：
     * - 事件在上报前会先落库，再加入内存队列。
     * - 上传成功后才从数据库/存储中删除。
     * - 适用场景：
     *   - 对事件可靠性要求高，不能丢失。
     *   - 支持离线场景或应用被杀进程后恢复事件。
     */
    ALWAYS_PERSIST,

    /**
     * BEST_EFFORT（尽力而为）：
     * - 事件先尝试直接上传，如果上传失败再落库。
     * - 可减少持久化操作，降低 I/O 开销。
     * - 适用场景：
     *   - 对事件丢失容忍度高的场景（例如日志类或非关键统计）。
     *   - 追求性能和轻量化的上传策略。
     */
    BEST_EFFORT
}
