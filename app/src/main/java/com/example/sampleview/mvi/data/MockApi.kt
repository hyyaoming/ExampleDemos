package com.example.sampleview.mvi.data

import kotlinx.coroutines.delay

class MockApi {

    suspend fun login(username: String, password: String): User {
        delay(1000) // 模拟网络延迟

        if (username == "admin" && password == "123456") {
            return User(username)
        } else {
            throw IllegalArgumentException("Invalid username or password")
        }
    }
}