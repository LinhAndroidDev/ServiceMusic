package com.example.serviceandroid.data.artist

import com.example.serviceandroid.data.auth.AuthRepository
import com.example.serviceandroid.model.Singer
import com.example.serviceandroid.utils.NetworkMonitor
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class FollowedSinger(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val createdAtMillis: Long = 0L,
)

sealed interface FollowMutationResult {
    data object Success : FollowMutationResult
    data object RequiresLogin : FollowMutationResult
    data object Offline : FollowMutationResult
    data class Failure(val message: String) : FollowMutationResult
}

interface FollowedSingerRepository {
    fun observeFollowed(): Flow<List<FollowedSinger>>
    fun observeIsFollowed(singerId: String): Flow<Boolean>
    suspend fun follow(singer: Singer): FollowMutationResult
    suspend fun unfollow(singerId: String): FollowMutationResult
}

@Singleton
class FollowedSingerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
) : FollowedSingerRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeFollowed(): Flow<List<FollowedSinger>> =
        authRepository.authState.flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else observeRemote(user.uid)
        }

    override fun observeIsFollowed(singerId: String): Flow<Boolean> =
        observeFollowed().map { singers -> singers.any { it.id == singerId } }

    override suspend fun follow(singer: Singer): FollowMutationResult {
        val userId = authRepository.currentUser()?.uid
        writePrecondition(userId)?.let { return it }
        if (singer.id.isBlank()) {
            return FollowMutationResult.Failure("Không thể quan tâm ca sĩ này")
        }
        return runCatching {
            singersCollection(userId.orEmpty()).document(singer.id).set(
                mapOf(
                    FIELD_NAME to singer.name,
                    FIELD_AVATAR_URL to singer.avatarUrl,
                    FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                ),
            ).await()
            FollowMutationResult.Success
        }.getOrElse {
            FollowMutationResult.Failure(it.message ?: "Không thể quan tâm ca sĩ")
        }
    }

    override suspend fun unfollow(singerId: String): FollowMutationResult {
        val userId = authRepository.currentUser()?.uid
        writePrecondition(userId)?.let { return it }
        if (singerId.isBlank()) {
            return FollowMutationResult.Failure("Không thể bỏ quan tâm ca sĩ này")
        }
        return runCatching {
            singersCollection(userId.orEmpty()).document(singerId).delete().await()
            FollowMutationResult.Success
        }.getOrElse {
            FollowMutationResult.Failure(it.message ?: "Không thể bỏ quan tâm ca sĩ")
        }
    }

    private fun writePrecondition(userId: String?): FollowMutationResult? = when {
        !networkMonitor.isOnlineNow() -> FollowMutationResult.Offline
        userId.isNullOrBlank() -> FollowMutationResult.RequiresLogin
        else -> null
    }

    private fun observeRemote(userId: String): Flow<List<FollowedSinger>> = callbackFlow {
        val registration = singersCollection(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val singers = snapshot?.documents.orEmpty()
                .mapNotNull(::toSinger)
                .sortedByDescending { it.createdAtMillis }
            trySend(singers)
        }
        awaitClose { registration.remove() }
    }

    private fun singersCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(FOLLOWED_SINGERS_COLLECTION)

    private fun toSinger(document: DocumentSnapshot): FollowedSinger? {
        val name = document.getString(FIELD_NAME)?.trim().orEmpty()
        if (document.id.isBlank() || name.isBlank()) return null
        return FollowedSinger(
            id = document.id,
            name = name,
            avatarUrl = document.getString(FIELD_AVATAR_URL).orEmpty(),
            createdAtMillis = document.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FOLLOWED_SINGERS_COLLECTION = "followedSingers"
        const val FIELD_NAME = "name"
        const val FIELD_AVATAR_URL = "avatarUrl"
        const val FIELD_CREATED_AT = "createdAt"
    }
}
