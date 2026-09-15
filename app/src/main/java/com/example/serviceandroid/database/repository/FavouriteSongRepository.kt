package com.example.serviceandroid.database.repository

import com.example.serviceandroid.data.auth.AuthRepository
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.NetworkMonitor
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

enum class ArrangeMusic {
    NEWEST,
    OLDEST,
    BY_NAME_SONG,
    BY_NAME_SINGLE
}

data class FavouriteSongRecord(
    val song: Song,
    val createdAtMillis: Long,
)

sealed interface FavouriteMutationResult {
    data object Success : FavouriteMutationResult
    data object RequiresLogin : FavouriteMutationResult
    data object Offline : FavouriteMutationResult
    data class Failure(val message: String) : FavouriteMutationResult
}

@Singleton
class FavouriteSongRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val records: StateFlow<List<FavouriteSongRecord>> = authRepository.authState
        .flatMapLatest { user ->
            if (user == null) {
                flow<List<FavouriteSongRecord>> { emit(emptyList()) }
            } else {
                flow {
                    emit(emptyList<FavouriteSongRecord>())
                    emitAll(observeRemote(user.uid))
                }
            }
        }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    fun observeFavouriteCount(): Flow<Int> =
        records.map { it.size }.distinctUntilChanged()

    fun observeAllRecords(): Flow<List<FavouriteSongRecord>> = records

    fun observeIsFavourite(songId: String): Flow<Boolean> =
        records.map { list -> list.any { it.song.id == songId } }.distinctUntilChanged()

    fun recordsToSortedSongs(
        source: List<FavouriteSongRecord>,
        typeArrangement: ArrangeMusic,
    ): MutableList<Song> = sortFavouriteRecords(source, typeArrangement)

    suspend fun insertSong(song: Song): FavouriteMutationResult {
        val userId = authRepository.currentUser()?.uid
        favouriteWritePrecondition(networkMonitor.isOnlineNow(), userId)?.let { return it }

        return runCatching {
            firestore.document(favouriteDocumentPath(userId.orEmpty(), song.id))
                .set(song.toFavouriteMap(), SetOptions.merge())
                .await()
            FavouriteMutationResult.Success
        }.getOrElse {
            FavouriteMutationResult.Failure(it.message ?: "Không thể thêm bài hát yêu thích")
        }
    }

    suspend fun deleteSongById(id: String): FavouriteMutationResult {
        val userId = authRepository.currentUser()?.uid
        favouriteWritePrecondition(networkMonitor.isOnlineNow(), userId)?.let { return it }

        return runCatching {
            firestore.document(favouriteDocumentPath(userId.orEmpty(), id)).delete().await()
            FavouriteMutationResult.Success
        }.getOrElse {
            FavouriteMutationResult.Failure(it.message ?: "Không thể xóa bài hát yêu thích")
        }
    }

    private fun observeRemote(userId: String): Flow<List<FavouriteSongRecord>> = callbackFlow {
        val registration = favouriteCollection(userId)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull(::toRecord))
            }
        awaitClose { registration.remove() }
    }

    private fun favouriteCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(FAVOURITE_SONGS_COLLECTION)

    private fun Song.toFavouriteMap(): Map<String, Any?> = mapOf(
        "songId" to id,
        "title" to title,
        "nameSinger" to nameSinger,
        "thumbnailUrl" to thumbnailUrl,
        "audioUrl" to audioUrl,
        "lyricUrl" to lyricUrl,
        "durationSec" to durationSec,
        "categoryId" to categoryId,
        "categoryName" to categoryName,
        "views" to views,
        FIELD_CREATED_AT to FieldValue.serverTimestamp(),
    )

    private fun toRecord(document: DocumentSnapshot): FavouriteSongRecord? {
        val songId = document.getString("songId").orEmpty().ifBlank { document.id }
        if (songId.isBlank()) return null
        return FavouriteSongRecord(
            song = Song(
                id = songId,
                title = document.getString("title").orEmpty(),
                nameSinger = document.getString("nameSinger").orEmpty(),
                thumbnailUrl = document.getString("thumbnailUrl").orEmpty(),
                audioUrl = document.getString("audioUrl").orEmpty(),
                lyricUrl = document.getString("lyricUrl").orEmpty(),
                durationSec = document.getLong("durationSec") ?: 0L,
                categoryId = document.getString("categoryId").orEmpty(),
                categoryName = document.getString("categoryName").orEmpty(),
                views = document.getLong("views") ?: 0L,
            ),
            createdAtMillis = document.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val FAVOURITE_SONGS_COLLECTION = "favouriteSongs"
        const val FIELD_CREATED_AT = "createdAt"
    }
}

internal fun favouriteWritePrecondition(
    isOnline: Boolean,
    userId: String?,
): FavouriteMutationResult? = when {
    !isOnline -> FavouriteMutationResult.Offline
    userId.isNullOrBlank() -> FavouriteMutationResult.RequiresLogin
    else -> null
}

internal fun favouriteDocumentPath(userId: String, songId: String): String =
    "users/$userId/favouriteSongs/$songId"

internal fun sortFavouriteRecords(
    source: List<FavouriteSongRecord>,
    typeArrangement: ArrangeMusic,
): MutableList<Song> {
    val collator = Collator.getInstance(Locale("vi", "VN")).apply {
        strength = Collator.PRIMARY
    }
    return when (typeArrangement) {
        ArrangeMusic.NEWEST -> source.sortedByDescending(FavouriteSongRecord::createdAtMillis)
        ArrangeMusic.OLDEST -> source.sortedBy(FavouriteSongRecord::createdAtMillis)
        ArrangeMusic.BY_NAME_SONG ->
            source.sortedWith { first, second ->
                collator.compare(first.song.title, second.song.title)
            }
        ArrangeMusic.BY_NAME_SINGLE ->
            source.sortedWith { first, second ->
                collator.compare(first.song.nameSinger, second.song.nameSinger)
            }
    }.map(FavouriteSongRecord::song).toMutableList()
}
