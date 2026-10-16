package com.MatthewBaerg.multitasked.data.repository

import com.MatthewBaerg.multitasked.data.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun currentUserId(): String?
    fun authStateChanges(): Flow<String?>
    fun logout()
    suspend fun login(email: String, password: String): String
    suspend fun register(email: String, password: String): String
    suspend fun getCurrentUser(): User?
    suspend fun updateUserName(newName: String)
}
