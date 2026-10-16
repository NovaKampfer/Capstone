package com.MatthewBaerg.multitasked.di

import com.MatthewBaerg.multitasked.data.repository.AuthRepository
import com.MatthewBaerg.multitasked.data.repository.AuthRepositoryImpl
import com.MatthewBaerg.multitasked.data.repository.BoardRepository
import com.MatthewBaerg.multitasked.data.repository.BoardRepositoryImpl
import com.MatthewBaerg.multitasked.data.repository.UserRepository
import com.MatthewBaerg.multitasked.data.repository.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBoardRepository(impl: BoardRepositoryImpl): BoardRepository
}
