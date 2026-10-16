package com.MatthewBaerg.multitasked.data.repository

import com.MatthewBaerg.multitasked.data.remote.FirebaseDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val remote: FirebaseDataSource
) : UserRepository {

    override suspend fun addFcmToken(userId: String, token: String) {
        remote.addFcmToken(userId, token)
    }

    override suspend fun removeFcmToken(userId: String, token: String) {
        remote.removeFcmToken(userId, token)
    }
}
