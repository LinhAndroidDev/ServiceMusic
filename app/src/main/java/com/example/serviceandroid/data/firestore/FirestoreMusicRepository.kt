package com.example.serviceandroid.data.firestore

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
        songs.document(id).get().await().toObject(FirestoreSong::class.java)

    override suspend fun getLatestSongs(limit: Long, fromServer: Boolean): List<FirestoreSong> =
        songs.orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get(if (fromServer) Source.SERVER else Source.DEFAULT)
            .await()
            .toObjects(FirestoreSong::class.java)

    override suspend fun getTopSongs(limit: Long, fromServer: Boolean): List<FirestoreSong> =
        songs.orderBy("views", Query.Direction.DESCENDING)
            .limit(limit)
            .get(if (fromServer) Source.SERVER else Source.DEFAULT)
            .await()
            .toObjects(FirestoreSong::class.java)

    override suspend fun getSinger(id: String): FirestoreSinger? {
        if (id.isBlank()) return null
        return singers.document(id).get().await().toObject(FirestoreSinger::class.java)
    }

    override suspend fun getSingers(): List<FirestoreSinger> =
        singers.orderBy("name").get().await().toObjects(FirestoreSinger::class.java)

    override suspend fun getCategories(): List<FirestoreCategory> =
        categories.orderBy("name").get().await().toObjects(FirestoreCategory::class.java)

    override suspend fun getSongsByCategory(categoryId: String, limit: Long): List<FirestoreSong> =
        songs.whereEqualTo("categoryId", categoryId)
            .limit(limit)
            .get()
            .await()
            .toObjects(FirestoreSong::class.java)

    override suspend fun getSongsBySinger(singerId: String, limit: Long): List<FirestoreSong> =
        songs.whereArrayContains("singerIds", singerId)
            .limit(limit)
            .get()
            .await()
            .toObjects(FirestoreSong::class.java)

    override suspend fun searchSongsByTitle(term: String, limit: Long): List<FirestoreSong> {
        val keyword = term.trim()
        if (keyword.isEmpty()) return emptyList()
        return songs.orderBy("title")
            .startAt(keyword)
            .endAt(keyword + "\uf8ff")
            .limit(limit)
            .get()
            .await()
            .toObjects(FirestoreSong::class.java)
    }

    override suspend fun incrementViews(songId: String) {
        songs.document(songId)
            .update("views", FieldValue.increment(1))
            .await()
    }

    override suspend fun getAdvertisements(fromServer: Boolean): List<FirestoreAdvertisement> {
        if (!fromServer) {
            advertisementCache?.let { return it }
        }
        return advertisements.orderBy("createdAt", Query.Direction.ASCENDING)
            .get(if (fromServer) Source.SERVER else Source.DEFAULT)
            .await()
            .toObjects(FirestoreAdvertisement::class.java)
            .also { advertisementCache = it }
    }

    override fun invalidateAdvertisementCache() {
        advertisementCache = null
    }
}
