package com.example.serviceandroid.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val authState: StateFlow<AuthUser?>
    fun currentUser(): AuthUser?
    suspend fun signInWithGoogle(idToken: String): AuthUser
    fun signOut()
}

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : AuthRepository {

    private val _authState = MutableStateFlow(firebaseAuth.currentUser?.toAuthUser())
    override val authState: StateFlow<AuthUser?> = _authState.asStateFlow()

    init {
        firebaseAuth.addAuthStateListener { auth ->
            _authState.value = auth.currentUser?.toAuthUser()
        }
    }

    override fun currentUser(): AuthUser? = _authState.value

    override suspend fun signInWithGoogle(idToken: String): AuthUser {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user
            ?: throw IllegalStateException("Firebase không trả về thông tin người dùng")

        return user.toAuthUser()
    }

    override fun signOut() {
        firebaseAuth.signOut()
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        displayName = displayName.orEmpty(),
        email = email.orEmpty(),
        photoUrl = photoUrl?.toString(),
    )
}
