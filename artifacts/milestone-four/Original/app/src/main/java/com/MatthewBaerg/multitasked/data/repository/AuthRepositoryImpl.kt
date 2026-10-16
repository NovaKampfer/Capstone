package com.MatthewBaerg.multitasked.data.repository

import com.MatthewBaerg.multitasked.data.model.User
import com.MatthewBaerg.multitasked.data.remote.FirebaseDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remote: FirebaseDataSource
) : AuthRepository {

    override fun currentUserId(): String? = remote.currentUserId()

    override fun authStateChanges(): Flow<String?> = remote.authStateChanges()

    override fun logout() {
        remote.logout()
    }

    override suspend fun login(email: String, password: String): String {
        return remote.login(email, password)
    }

    override suspend fun register(email: String, password: String): String {
        return remote.register(email, password)
    }

    override suspend fun getCurrentUser(): User? {
        return remote.getCurrentUser()
    }

    override suspend fun updateUserName(newName: String) {
        remote.updateUserName(newName)
    }
}
