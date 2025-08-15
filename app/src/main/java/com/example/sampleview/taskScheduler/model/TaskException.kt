/**
 * 表示任务调度过程中发生的异常。
 *
 * 该异常用于封装任务执行、依赖失败、重试中断等运行时错误，
 * 并提供统一的异常类型以便调度器或上层统一处理。
 *
 * @param message 异常描述信息，通常用于日志和错误提示。
 * @param cause 原始异常，用于保留堆栈信息和错误链。
 *
 * @see Exception
 */
class TaskException(message: String, cause: Throwable? = null) : Exception(message, cause)
