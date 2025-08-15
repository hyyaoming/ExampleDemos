package com.example.sampleview.mvi.data

class LoginRepository {

    private val api = MockApi()

    suspend fun login(username: String, password: String): Result<User> {
        return runCatching { api.login(username, password) }
    }

}