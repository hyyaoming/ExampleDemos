package com.example.sampleview.mvi.ui

import com.example.sampleview.mvi.base.UiEffect
import com.example.sampleview.mvi.base.UiEvent
import com.example.sampleview.mvi.base.UiState


object LoginContract {
    data class State(
        val username: String = "",
        val password: String = "",
        val loginUiState: LoginUiState = LoginUiState.Idle,
    ) : UiState

    sealed class Effect : UiEffect {
        object LoginSuccess : Effect()
        data class ShowError(val message: String) : Effect()
    }

    sealed class Event : UiEvent {
        data class UsernameChanged(val value: String) : Event()
        data class PasswordChanged(val value: String) : Event()
        object SubmitLogin : Event()
    }
}
