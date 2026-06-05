package com.example.serviceandroid.di

import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.data.firestore.FirestoreMusicRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FirestoreRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFirestoreMusicRepository(
        impl: FirestoreMusicRepositoryImpl,
    ): FirestoreMusicRepository
}
