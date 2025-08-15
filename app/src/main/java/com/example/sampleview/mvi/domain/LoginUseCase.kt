package com.example.sampleview.mvi.domain

import com.example.sampleview.mvi.base.FlowUseCase
import com.example.sampleview.mvi.data.LoginRepository
import com.example.sampleview.mvi.data.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LoginUseCase : FlowUseCase<LoginUseCase.Params, User>() {

    private val repository = LoginRepository()

    data class Params(val username: String, val password: String)

    override fun execute(params: Params): Flow<Result<User>> = flow {
        if (params.username.isBlank() || params.password.isBlank()) {
            emit(Result.failure(Exception("用户名或密码不能为空")))
            return@flow
        }
        emit(repository.login(params.username, params.password))
    }
}