package com.example.serviceandroid.data.user

import com.example.serviceandroid.data.auth.AuthUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    suspend fun ensureProfile(user: AuthUser)
    suspend fun upsertAfterLogin(user: AuthUser)
}

@Singleton
class FirestoreUserRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : UserRepository {

    override suspend fun ensureProfile(user: AuthUser) {
        upsertProfile(user, recordLogin = false)
    }

    override suspend fun upsertAfterLogin(user: AuthUser) {
        upsertProfile(user, recordLogin = true)
    }

    private suspend fun upsertProfile(user: AuthUser, recordLogin: Boolean) {
        require(user.uid.isNotBlank()) { "UID người dùng không hợp lệ" }

        val userDocument = firestore.collection(USERS_COLLECTION).document(user.uid)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(userDocument)
            val profile = mutableMapOf(
                "uid" to user.uid,
                "displayName" to user.displayName,
                "email" to user.email,
                "photoUrl" to user.photoUrl,
                "provider" to GOOGLE_PROVIDER,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (recordLogin) {
                profile["lastLoginAt"] = FieldValue.serverTimestamp()
            }
            if (!snapshot.exists()) {
                profile["createdAt"] = FieldValue.serverTimestamp()
            }

            transaction.set(userDocument, profile, SetOptions.merge())
        }.await()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val GOOGLE_PROVIDER = "google.com"
    }
}
