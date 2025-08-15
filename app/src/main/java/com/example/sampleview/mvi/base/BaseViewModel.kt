package com.example.sampleview.mvi.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<State : UiState, Event : UiEvent, Effect : UiEffect?> : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<State> = _uiState

    private val _effect = MutableSharedFlow<Effect>()
    val effect: Flow<Effect> = _effect.asSharedFlow()

    abstract fun initialState(): State

    protected fun setState(reducer: State.() -> State) {
        _uiState.value = _uiState.value.reducer()
    }

    protected fun sendEffect(builder: () -> Effect) {
        viewModelScope.launch {
            _effect.emit(builder())
        }
    }

    abstract fun handleEvent(event: Event)
}
