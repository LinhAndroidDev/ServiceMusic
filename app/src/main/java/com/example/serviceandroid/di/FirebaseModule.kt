package com.example.serviceandroid.di

import com.example.serviceandroid.data.auth.AuthRepository
import com.example.serviceandroid.data.auth.FirebaseAuthRepository
import com.example.serviceandroid.data.firestore.FirestoreMusicRepository
import com.example.serviceandroid.data.firestore.FirestoreMusicRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
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

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FirestoreRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFirestoreMusicRepository(
        impl: FirestoreMusicRepositoryImpl,
    ): FirestoreMusicRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: FirebaseAuthRepository,
    ): AuthRepository
}
