package com.MatthewBaerg.multitasked.di

import android.content.Context
import com.MatthewBaerg.multitasked.data.remote.FirebaseDataSource
import com.MatthewBaerg.multitasked.ui.CelebrationSettings
import com.MatthewBaerg.multitasked.ui.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import com.google.firebase.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.MatthewBaerg.multitasked.ui.tasks.TaskClock

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @SortingDispatcher
    fun provideSortingDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    fun provideTaskClock(): TaskClock = TaskClock { System.currentTimeMillis() }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDataSource(auth: FirebaseAuth, db: FirebaseFirestore): FirebaseDataSource =
        FirebaseDataSource(auth, db)

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository =
        SettingsRepository(context)

    @Provides
    fun provideCelebrationSettings(
        settingsRepository: SettingsRepository
    ): CelebrationSettings = settingsRepository

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return Firebase.functions
    }
}
