package com.example.serviceandroid.data.firestore

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface FirestoreMusicRepository {
    suspend fun getSong(id: String): FirestoreSong?
    suspend fun getLatestSongs(limit: Long = 50, fromServer: Boolean = false): List<FirestoreSong>
    suspend fun getTopSongs(limit: Long = 50, fromServer: Boolean = false): List<FirestoreSong>
    suspend fun getSinger(id: String): FirestoreSinger?
    suspend fun getSingers(): List<FirestoreSinger>
    suspend fun getCategories(): List<FirestoreCategory>
    suspend fun getSongsByCategory(categoryId: String, limit: Long = 50): List<FirestoreSong>
    suspend fun getSongsBySinger(singerId: String, limit: Long = 50): List<FirestoreSong>
    suspend fun searchSongsByTitle(term: String, limit: Long = 20): List<FirestoreSong>
    suspend fun incrementViews(songId: String)
    /** Banners — oldest first ([Query.Direction.ASCENDING] on `createdAt`). */
    suspend fun getAdvertisements(fromServer: Boolean = false): List<FirestoreAdvertisement>
    fun invalidateAdvertisementCache()
}

@Singleton
class FirestoreMusicRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
) : FirestoreMusicRepository {

    private val songs get() = db.collection("songs")
    private val singers get() = db.collection("singers")
    private val categories get() = db.collection("categories")
    private val advertisements get() = db.collection("advertisements")

    @Volatile
    private var advertisementCache: List<FirestoreAdvertisement>? = null

    override suspend fun getSong(id: String): FirestoreSong? =
        fetchDocument(id) { songs.document(it) }

    override suspend fun getLatestSongs(limit: Long, fromServer: Boolean): List<FirestoreSong> =
        fetchQuery(fromServer) {
            songs.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get(it)
                .await()
                .toObjects(FirestoreSong::class.java)
        }

    override suspend fun getTopSongs(limit: Long, fromServer: Boolean): List<FirestoreSong> =
        fetchQuery(fromServer) {
            songs.orderBy("views", Query.Direction.DESCENDING)
                .limit(limit)
                .get(it)
                .await()
                .toObjects(FirestoreSong::class.java)
        }

    override suspend fun getSinger(id: String): FirestoreSinger? {
        if (id.isBlank()) return null
        return fetchDocument(id) { singers.document(it) }
    }

    override suspend fun getSingers(): List<FirestoreSinger> =
        fetchQuery {
            singers.orderBy("name")
                .get(it)
                .await()
                .toObjects(FirestoreSinger::class.java)
        }

    override suspend fun getCategories(): List<FirestoreCategory> =
        fetchQuery {
            categories.orderBy("name")
                .get(it)
                .await()
                .toObjects(FirestoreCategory::class.java)
        }

    override suspend fun getSongsByCategory(categoryId: String, limit: Long): List<FirestoreSong> =
        fetchQuery {
            songs.whereEqualTo("categoryId", categoryId)
                .limit(limit)
                .get(it)
                .await()
                .toObjects(FirestoreSong::class.java)
        }

    override suspend fun getSongsBySinger(singerId: String, limit: Long): List<FirestoreSong> =
        fetchQuery {
            songs.whereArrayContains("singerIds", singerId)
                .limit(limit)
                .get(it)
                .await()
                .toObjects(FirestoreSong::class.java)
        }

    override suspend fun searchSongsByTitle(term: String, limit: Long): List<FirestoreSong> {
        val keyword = term.trim()
        if (keyword.isEmpty()) return emptyList()
        return fetchQuery {
            songs.orderBy("title")
                .startAt(keyword)
                .endAt(keyword + "\uf8ff")
                .limit(limit)
                .get(it)
                .await()
                .toObjects(FirestoreSong::class.java)
        }
    }

    override suspend fun incrementViews(songId: String) {
        runCatching {
            songs.document(songId)
                .update("views", FieldValue.increment(1))
                .await()
        }
    }

    override suspend fun getAdvertisements(fromServer: Boolean): List<FirestoreAdvertisement> {
        if (!fromServer) {
            advertisementCache?.let { return it }
        }
        return fetchQuery(fromServer) {
            advertisements.orderBy("createdAt", Query.Direction.ASCENDING)
                .get(it)
                .await()
                .toObjects(FirestoreAdvertisement::class.java)
        }.also { fetched ->
            if (fetched.isNotEmpty()) {
                advertisementCache = fetched
            }
        }
    }

    override fun invalidateAdvertisementCache() {
        advertisementCache = null
    }

    private suspend inline fun <reified T> fetchDocument(
        id: String,
        crossinline document: (String) -> DocumentReference,
    ): T? {
        val primarySource = Source.DEFAULT
        return try {
            document(id).get(primarySource).await().toObject(T::class.java)
        } catch (e: FirebaseFirestoreException) {
            if (shouldFallbackToCache(e)) {
                runCatching {
                    document(id).get(Source.CACHE).await().toObject(T::class.java)
                }.getOrNull()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private suspend inline fun <T> fetchQuery(
        preferServer: Boolean = false,
        crossinline block: suspend (Source) -> List<T>,
    ): List<T> {
        val primarySource = if (preferServer) Source.SERVER else Source.DEFAULT
        return try {
            block(primarySource)
        } catch (e: FirebaseFirestoreException) {
            if (shouldFallbackToCache(e)) {
                runCatching { block(Source.CACHE) }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun shouldFallbackToCache(error: FirebaseFirestoreException): Boolean =
        error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
            error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
}
