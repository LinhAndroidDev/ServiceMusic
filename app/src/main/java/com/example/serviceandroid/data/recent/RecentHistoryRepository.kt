package com.example.serviceandroid.data.recent

import com.example.serviceandroid.data.auth.AuthRepository
import com.example.serviceandroid.database.RecentSongEntity
import com.example.serviceandroid.database.dao.RecentSongDao
import com.example.serviceandroid.model.Song
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

interface RecentHistoryRepository {
    fun observeRecentSongs(limit: Int = 20): Flow<List<Song>>
    suspend fun recordSong(song: Song)
    suspend fun hasLocalHistory(): Boolean
    suspend fun syncLocalHistory(userId: String)
    suspend fun discardLocalHistory()
}

@Singleton
class RecentHistoryRepositoryImpl @Inject constructor(
    private val dao: RecentSongDao,
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
) : RecentHistoryRepository {

    override fun observeRecentSongs(limit: Int): Flow<List<Song>> =
        authRepository.authState.flatMapLatest { user ->
            if (user == null) {
                dao.observeLatest(limit).map { entities -> entities.map(RecentSongEntity::toSong) }
            } else {
                observeRemote(user.uid, limit)
            }
        }

    override suspend fun recordSong(song: Song) {
        if (song.id.isBlank()) return
        val userId = authRepository.currentUser()?.uid
        if (userId == null) {
            val existing = dao.getById(song.id)
            dao.upsert(
                RecentSongEntity.fromSong(
                    song = song,
                    playCount = (existing?.playCount ?: 0L) + 1L,
                )
            )
            dao.trimTo(MAX_LOCAL_ITEMS)
        } else {
            recentCollection(userId)
                .document(song.id)
                .set(
                    song.toRemoteMap(
                        lastPlayedAt = FieldValue.serverTimestamp(),
                        playCount = FieldValue.increment(1L),
                    ),
                    SetOptions.merge(),
                )
                .await()
        }
    }

    override suspend fun hasLocalHistory(): Boolean = dao.count() > 0

    override suspend fun syncLocalHistory(userId: String) {
        require(userId.isNotBlank()) { "UID người dùng không hợp lệ" }
        val localHistory = dao.getAll()
        if (localHistory.isEmpty()) return

        val collection = recentCollection(userId)
        val remoteById = collection.get().await().documents.associateBy { it.id }
        val batch = firestore.batch()

        localHistory.forEach { local ->
            val remote = remoteById[local.songId]
            val remoteLastPlayedAt = remote?.getTimestamp(FIELD_LAST_PLAYED_AT)
                ?.toDate()
                ?.time
                ?: Long.MIN_VALUE
            val values = mutableMapOf<String, Any?>(
                FIELD_PLAY_COUNT to FieldValue.increment(local.playCount),
            )
            if (local.lastPlayedAt >= remoteLastPlayedAt) {
                values.putAll(
                    local.toSong().toRemoteMap(
                        lastPlayedAt = Timestamp(Date(local.lastPlayedAt)),
                        playCount = FieldValue.increment(local.playCount),
                    )
                )
            }
            batch.set(collection.document(local.songId), values, SetOptions.merge())
        }

        batch.commit().await()
        dao.deleteAll()
    }

    override suspend fun discardLocalHistory() {
        dao.deleteAll()
    }

    private fun observeRemote(userId: String, limit: Int): Flow<List<Song>> = callbackFlow {
        val registration = recentCollection(userId)
            .orderBy(FIELD_LAST_PLAYED_AT, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull(::toSong))
            }
        awaitClose { registration.remove() }
    }

    private fun recentCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(RECENT_SONGS_COLLECTION)

    private fun Song.toRemoteMap(
        lastPlayedAt: Any,
        playCount: Any,
    ): Map<String, Any?> = mapOf(
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
        FIELD_LAST_PLAYED_AT to lastPlayedAt,
        FIELD_PLAY_COUNT to playCount,
    )

    private fun toSong(document: DocumentSnapshot): Song? {
        val songId = document.getString("songId").orEmpty().ifBlank { document.id }
        if (songId.isBlank()) return null
        return Song(
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
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val RECENT_SONGS_COLLECTION = "recentSongs"
        const val FIELD_LAST_PLAYED_AT = "lastPlayedAt"
        const val FIELD_PLAY_COUNT = "playCount"
        const val MAX_LOCAL_ITEMS = 100
    }
}
