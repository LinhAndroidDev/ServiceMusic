package com.example.serviceandroid.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
)

interface AuthRepository {
    fun currentUser(): AuthUser?
    suspend fun signInWithGoogle(idToken: String): AuthUser
    fun signOut()
}

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    override fun currentUser(): AuthUser? = firebaseAuth.currentUser?.let { user ->
        AuthUser(
            uid = user.uid,
            displayName = user.displayName.orEmpty(),
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString(),
        )
    }

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user
            ?: throw IllegalStateException("Firebase không trả về thông tin người dùng")

        return AuthUser(
            uid = user.uid,
            displayName = user.displayName.orEmpty(),
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString(),
        )
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }
}
