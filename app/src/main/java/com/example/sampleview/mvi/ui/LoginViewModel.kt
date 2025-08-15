package com.example.sampleview.mvi.ui

import androidx.lifecycle.viewModelScope
import com.example.sampleview.mvi.base.BaseViewModel
import com.example.sampleview.mvi.domain.LoginUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

class LoginViewModel : BaseViewModel<LoginContract.State, LoginContract.Event, LoginContract.Effect>() {
    private val loginUseCase = LoginUseCase()

    override fun initialState(): LoginContract.State = LoginContract.State()

    override fun handleEvent(event: LoginContract.Event) {
        when (event) {
            is LoginContract.Event.UsernameChanged -> {
                setState { copy(username = event.value) }
            }
            is LoginContract.Event.PasswordChanged -> {
                setState { copy(password = event.value) }
            }
            is LoginContract.Event.SubmitLogin -> {
                handleLogin()
            }
        }
    }

    private fun handleLogin() {
        loginUseCase(LoginUseCase.Params(uiState.value.username, uiState.value.password))
            .onStart {
                setState { copy(loginUiState = LoginUiState.Loading) }
            }
            .onEach { result ->
                result.onSuccess {
                    sendEffect { LoginContract.Effect.LoginSuccess }
                }.onFailure {
                    sendEffect { LoginContract.Effect.ShowError(it.message ?: "登录失败") }
                }
            }
            .onCompletion {
                setState { copy(loginUiState = LoginUiState.Idle) }
            }
            .launchIn(viewModelScope)
    }
}
