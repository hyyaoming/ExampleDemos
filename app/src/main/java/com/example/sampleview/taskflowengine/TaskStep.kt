package com.example.sampleview.taskflowengine

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface TaskStep<I, O> {
    val stepName: String
    val dispatcher: CoroutineDispatcher get() = Dispatchers.Default
    suspend fun execute(input: I): O
    val timeoutMillis: Long get() = 0L
}