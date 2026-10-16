package com.baerg.multitasked.data.repository

interface UserRepository {
    suspend fun addFcmToken(userId: String, token: String)
    suspend fun removeFcmToken(userId: String, token: String)
}
