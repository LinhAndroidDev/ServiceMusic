package com.example.serviceandroid.data.playlist

import com.example.serviceandroid.data.auth.AuthRepository
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.NetworkMonitor
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

sealed interface PlaylistMutationResult {
    data class Success(val playlistId: String = "") : PlaylistMutationResult
    data object AlreadyExists : PlaylistMutationResult
    data object RequiresLogin : PlaylistMutationResult
    data object Offline : PlaylistMutationResult
    data class Failure(val message: String) : PlaylistMutationResult
}

interface PlaylistRepository {
    fun observePlaylists(): Flow<List<UserPlaylist>>
    fun observePlaylist(playlistId: String): Flow<UserPlaylist?>
    fun observeSongs(playlistId: String): Flow<List<Song>>
    suspend fun createPlaylist(title: String, isPublic: Boolean = true): PlaylistMutationResult
    suspend fun addSong(
        playlistId: String,
        song: Song,
        currentCoverUrl: String = "",
    ): PlaylistMutationResult
    suspend fun updatePlaylist(
        playlistId: String,
        title: String,
        isPublic: Boolean,
    ): PlaylistMutationResult
    suspend fun reorderSongs(
        playlistId: String,
        songIds: List<String>,
        firstCoverUrl: String,
    ): PlaylistMutationResult
    suspend fun deletePlaylist(playlistId: String): PlaylistMutationResult
    suspend fun removeSong(playlistId: String, songId: String): PlaylistMutationResult
}

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor,
) : PlaylistRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePlaylists(): Flow<List<UserPlaylist>> =
        authRepository.authState.flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else observePlaylistsRemote(user.uid)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePlaylist(playlistId: String): Flow<UserPlaylist?> =
        authRepository.authState.flatMapLatest { user ->
            if (user == null || playlistId.isBlank()) flowOf(null)
            else observePlaylistRemote(user.uid, playlistId)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSongs(playlistId: String): Flow<List<Song>> =
        authRepository.authState.flatMapLatest { user ->
            if (user == null || playlistId.isBlank()) flowOf(emptyList())
            else observeSongsRemote(user.uid, playlistId)
        }

    override suspend fun createPlaylist(
        title: String,
        isPublic: Boolean,
    ): PlaylistMutationResult {
        val name = title.trim()
        val userId = authRepository.currentUser()?.uid
        playlistWritePrecondition(networkMonitor.isOnlineNow(), userId)?.let { return it }
        if (name.isBlank()) {
            return PlaylistMutationResult.Failure("Tên playlist không được để trống")
        }

        return runCatching {
            val doc = playlistsCollection(userId.orEmpty()).document()
            doc.set(
                mapOf(
                    FIELD_TITLE to name,
                    FIELD_IS_PUBLIC to isPublic,
                    FIELD_SONG_COUNT to 0,
                    FIELD_COVER_URL to "",
                    FIELD_CREATED_AT to FieldValue.serverTimestamp(),
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            ).await()
            PlaylistMutationResult.Success(doc.id)
        }.getOrElse {
            PlaylistMutationResult.Failure(it.message ?: "Không thể tạo playlist")
        }
    }

    override suspend fun addSong(
        playlistId: String,
        song: Song,
        currentCoverUrl: String,
    ): PlaylistMutationResult {
        val userId = authRepository.currentUser()?.uid
        playlistWritePrecondition(networkMonitor.isOnlineNow(), userId)?.let { return it }
        if (playlistId.isBlank() || song.id.isBlank()) {
            return PlaylistMutationResult.Failure("Không thể thêm bài hát vào playlist")
        }

        return runCatching {
            val playlistRef = playlistsCollection(userId.orEmpty()).document(playlistId)
            val songsRef = playlistRef.collection(SONGS_COLLECTION)
            val existing = songsRef.get().await()
            if (existing.documents.any { it.id == song.id }) {
                return PlaylistMutationResult.AlreadyExists
            }
            val nextOrder = existing.documents.maxOfOrNull { songOrder(it) }?.plus(1) ?: 0L
            val updates = hashMapOf<String, Any>(
                FIELD_SONG_COUNT to FieldValue.increment(1),
                FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            )
            if (currentCoverUrl.isBlank() && song.thumbnailUrl.isNotBlank()) {
                updates[FIELD_COVER_URL] = song.thumbnailUrl
            }
            val batch = firestore.batch()
            batch.set(
                songsRef.document(song.id),
                song.toPlaylistSongMap(nextOrder),
                SetOptions.merge(),
            )
            batch.update(playlistRef, updates)
            batch.commit().await()
            PlaylistMutationResult.Success(playlistId)
        }.getOrElse {
            PlaylistMutationResult.Failure(it.message ?: "Không thể thêm bài hát vào playlist")
        }
    }

    override suspend fun updatePlaylist(
        playlistId: String,
        title: String,
        isPublic: Boolean,
    ): PlaylistMutationResult {
        val name = title.trim()
        val userId = authRepository.currentUser()?.uid
        playlistWritePrecondition(networkMonitor.isOnlineNow(), userId)?.let { return it }
        if (playlistId.isBlank()) {
            return PlaylistMutationResult.Failure("Playlist không tồn tại")
        }
        if (name.isBlank()) {
            return PlaylistMutationResult.Failure("Tên playlist không được để trống")
        }
        return runCatching {
            playlistsCollection(userId.orEmpty()).document(playlistId)
                .update(
                    mapOf(
                        FIELD_TITLE to name,
                        FIELD_IS_PUBLIC to isPublic,
                        FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                    ),
                )
                .await()
            PlaylistMutationResult.Success(playlistId)
        }.getOrElse {
            PlaylistMutationResult.Failure(it.message ?: "Không thể cập nhật playlist")
        }
    }

    override suspend fun reorderSongs(
        playlistId: String,
        songIds: List<String>,
        firstCoverUrl: String,
    ): PlaylistMutationResult {
        val userId = authRepository.currentUser()?.uid
        playlistWritePrecondition(networkMonitor.isOnlineNow(), userId)?.let { return it }
        if (playlistId.isBlank()) {
            return PlaylistMutationResult.Failure("Playlist không tồn tại")
        }
        return runCatching {
            val playlistRef = playlistsCollection(userId.orEmpty()).document(playlistId)
            val songsRef = playlistRef.collection(SONGS_COLLECTION)
            val batch = firestore.batch()
            songIds.forEachIndexed { index, songId ->
                if (songId.isNotBlank()) {
                    batch.update(songsRef.document(songId), mapOf(FIELD_ORDER to index.toLong()))
                }
            }
            val playlistUpdates = hashMapOf<String, Any>(
                FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
            )
            if (firstCoverUrl.isNotBlank()) {
                playlistUpdates[FIELD_COVER_URL] = firstCoverUrl
            }
            batch.update(playlistRef, playlistUpdates)
            batch.commit().await()
            PlaylistMutationResult.Success(playlistId)
        }.getOrElse {
            PlaylistMutationResult.Failure(it.message ?: "Không thể sắp xếp playlist")
        }
    }

    override suspend fun deletePlaylist(playlistId: String): PlaylistMutationResult {
        val userId = authRepository.currentUser()?.uid
        playlistWritePrecondition(networkMonitor.isOnlineNow(), userId)?.let { return it }
        if (playlistId.isBlank()) {
            return PlaylistMutationResult.Failure("Playlist không tồn tại")
        }
        return runCatching {
            val playlistRef = playlistsCollection(userId.orEmpty()).document(playlistId)
            val songs = playlistRef.collection(SONGS_COLLECTION).get().await()
            songs.documents.chunked(400).forEach { chunk ->
                val batch = firestore.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            playlistRef.delete().await()
            PlaylistMutationResult.Success(playlistId)
        }.getOrElse {
            PlaylistMutationResult.Failure(it.message ?: "Không thể xóa playlist")
        }
    }

    override suspend fun removeSong(
        playlistId: String,
        songId: String,
    ): PlaylistMutationResult {
        val userId = authRepository.currentUser()?.uid
        playlistWritePrecondition(networkMonitor.isOnlineNow(), userId)?.let { return it }
        if (playlistId.isBlank() || songId.isBlank()) {
            return PlaylistMutationResult.Failure("Không thể xoá bài hát khỏi playlist")
        }
        return runCatching {
            val playlistRef = playlistsCollection(userId.orEmpty()).document(playlistId)
            val songsRef = playlistRef.collection(SONGS_COLLECTION)
            val existing = songsRef.get().await()
            val remaining = existing.documents.filter { it.id != songId }
            if (remaining.size == existing.size()) {
                return PlaylistMutationResult.Failure("Bài hát không có trong playlist")
            }
            val nextCover = remaining
                .mapNotNull(::toSongRecord)
                .sortedWith(compareBy<PlaylistSongRecord> { it.order }.thenBy { it.addedAt })
                .firstOrNull()
                ?.song
                ?.thumbnailUrl
                .orEmpty()
            val batch = firestore.batch()
            batch.delete(songsRef.document(songId))
            batch.update(
                playlistRef,
                mapOf(
                    FIELD_SONG_COUNT to FieldValue.increment(-1),
                    FIELD_COVER_URL to nextCover,
                    FIELD_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            batch.commit().await()
            PlaylistMutationResult.Success(playlistId)
        }.getOrElse {
            PlaylistMutationResult.Failure(it.message ?: "Không thể xoá bài hát khỏi playlist")
        }
    }

    private fun observePlaylistsRemote(userId: String): Flow<List<UserPlaylist>> = callbackFlow {
        val registration = playlistsCollection(userId)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull(::toPlaylist))
            }
        awaitClose { registration.remove() }
    }

    private fun observePlaylistRemote(
        userId: String,
        playlistId: String,
    ): Flow<UserPlaylist?> = callbackFlow {
        val registration = playlistsCollection(userId)
            .document(playlistId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.let(::toPlaylist))
            }
        awaitClose { registration.remove() }
    }

    private fun observeSongsRemote(userId: String, playlistId: String): Flow<List<Song>> =
        callbackFlow {
            val registration = playlistsCollection(userId)
                .document(playlistId)
                .collection(SONGS_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val songs = snapshot?.documents.orEmpty()
                        .mapNotNull(::toSongRecord)
                        .sortedWith(compareBy<PlaylistSongRecord> { it.order }.thenBy { it.addedAt })
                        .map { it.song }
                    trySend(songs)
                }
            awaitClose { registration.remove() }
        }

    private fun playlistsCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(PLAYLISTS_COLLECTION)

    private fun Song.toPlaylistSongMap(order: Long): Map<String, Any?> = mapOf(
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
        FIELD_ORDER to order,
        FIELD_ADDED_AT to FieldValue.serverTimestamp(),
    )

    private fun songOrder(document: DocumentSnapshot): Long {
        val order = document.getLong(FIELD_ORDER)
        if (order != null) return order
        return document.getTimestamp(FIELD_ADDED_AT)?.toDate()?.time ?: Long.MAX_VALUE
    }

    private fun toSongRecord(document: DocumentSnapshot): PlaylistSongRecord? {
        val song = toSong(document) ?: return null
        return PlaylistSongRecord(
            song = song,
            order = document.getLong(FIELD_ORDER) ?: Long.MAX_VALUE,
            addedAt = document.getTimestamp(FIELD_ADDED_AT)?.toDate()?.time ?: 0L,
        )
    }

    private fun toPlaylist(document: DocumentSnapshot): UserPlaylist? {
        val title = document.getString(FIELD_TITLE).orEmpty()
        if (document.id.isBlank() || title.isBlank()) return null
        return UserPlaylist(
            id = document.id,
            title = title,
            isPublic = document.getBoolean(FIELD_IS_PUBLIC) ?: true,
            songCount = (document.getLong(FIELD_SONG_COUNT) ?: 0L).toInt(),
            coverUrl = document.getString(FIELD_COVER_URL).orEmpty(),
            createdAt = document.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: 0L,
        )
    }

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
        const val PLAYLISTS_COLLECTION = "playlists"
        const val SONGS_COLLECTION = "songs"
        const val FIELD_TITLE = "title"
        const val FIELD_IS_PUBLIC = "isPublic"
        const val FIELD_SONG_COUNT = "songCount"
        const val FIELD_COVER_URL = "coverUrl"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
        const val FIELD_ADDED_AT = "addedAt"
        const val FIELD_ORDER = "order"
    }
}

private data class PlaylistSongRecord(
    val song: Song,
    val order: Long,
    val addedAt: Long,
)

internal fun playlistWritePrecondition(
    isOnline: Boolean,
    userId: String?,
): PlaylistMutationResult? = when {
    !isOnline -> PlaylistMutationResult.Offline
    userId.isNullOrBlank() -> PlaylistMutationResult.RequiresLogin
    else -> null
}
