package com.example.sampleview.log


interface ILogFilter {
    fun shouldWrite(log: LogEntry): Boolean
}

class FilterChain : ILogFilter {
    private val filters = mutableListOf<ILogFilter>()

    fun addFilter(filter: ILogFilter) = filters.add(filter)

    fun removeFilter(filter: ILogFilter) = filters.remove(filter)

    override fun shouldWrite(log: LogEntry): Boolean {
        return filters.all { it.shouldWrite(log) }
    }
}

class LevelFilter(private val minLevel: String) : ILogFilter {
    private val levels = listOf("DEBUG", "INFO", "WARN", "ERROR")
    override fun shouldWrite(log: LogEntry): Boolean {
        val logIndex = levels.indexOf(log.level)
        val minIndex = levels.indexOf(minLevel)
        return logIndex >= minIndex
    }
}
