package com.example.sampleview.mvi.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

abstract class BaseActivity<VM : BaseViewModel<S, E, F>, S : UiState, E : UiEvent, F : UiEffect?> : AppCompatActivity() {

    protected abstract val viewModel: VM

    protected abstract fun renderState(state: S)
    protected abstract fun handleEffect(effect: F)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observeState()
        observeEffect()
    }

    private fun observeState() {
        lifecycleScope.launchWhenStarted {
            viewModel.uiState.collect {
                renderState(it)
            }
        }
    }

    private fun observeEffect() {
        lifecycleScope.launchWhenStarted {
            viewModel.effect.collect {
                it?.let(::handleEffect)
            }
        }
    }
}
