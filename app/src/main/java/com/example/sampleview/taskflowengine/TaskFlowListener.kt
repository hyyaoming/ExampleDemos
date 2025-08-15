package com.example.sampleview.taskflowengine

interface TaskFlowListener<I, O> {
    fun onChainStart()
    fun onChainComplete(finalResult: O)
    fun onChainFailure(stepName: String, error: Throwable)
}