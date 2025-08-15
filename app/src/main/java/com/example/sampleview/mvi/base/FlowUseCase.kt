package com.example.sampleview.mvi.base

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

abstract class FlowUseCase<in P, R> {
    operator fun invoke(params: P): Flow<Result<R>> =
        execute(params)
            .catch { e -> emit(Result.failure(e)) }

    protected abstract fun execute(params: P): Flow<Result<R>>
}
