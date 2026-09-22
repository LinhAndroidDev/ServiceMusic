package com.example.serviceandroid.data.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface FirestoreMusicRepository {
    suspend fun getSong(id: String): FirestoreSong?
    suspend fun getLatestSongs(limit: Long = 50, fromServer: Boolean = false): List<FirestoreSong>
    suspend fun getTopSongs(limit: Long = 50, fromServer: Boolean = false): List<FirestoreSong>
    suspend fun getSinger(id: String): FirestoreSinger?
    suspend fun getSingers(): List<FirestoreSinger>
    /** Categories — oldest first via REST `createTime` (collection has no `createdAt` field). */
    suspend fun getCategories(): List<FirestoreCategory>
    suspend fun getSongsByCategory(categoryId: String, limit: Long = 50): List<FirestoreSong>
    suspend fun getSongsBySinger(singerId: String, limit: Long = 50): List<FirestoreSong>
    suspend fun searchSongsByTitle(term: String, limit: Long = 20): List<FirestoreSong>
    suspend fun searchSingersByName(term: String, limit: Long = 20): List<FirestoreSinger>
    suspend fun incrementViews(songId: String)
    /** Banners — oldest first ([Query.Direction.ASCENDING] on `createdAt`). */
    suspend fun getAdvertisements(fromServer: Boolean = false): List<FirestoreAdvertisement>
    fun invalidateAdvertisementCache()
}

@Singleton
class FirestoreMusicRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
) : FirestoreMusicRepository {

    private val songs get() = db.collection("songs")
    private val singers get() = db.collection("singers")
    private val categories get() = db.collection("categories")
    private val advertisements get() = db.collection("advertisements")

    @Volatile
    private var advertisementCache: List<FirestoreAdvertisement>? = null

    @Volatile
    private var categoryCache: List<FirestoreCategory>? = null

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

    override suspend fun getCategories(): List<FirestoreCategory> {
        categoryCache?.let { return it }
        val ordered = runCatching { fetchCategoriesOrderedByCreateTime() }.getOrDefault(emptyList())
        val categoriesResult = ordered.ifEmpty {
            fetchQuery {
                categories.get(it)
                    .await()
                    .toObjects(FirestoreCategory::class.java)
            }
        }
        if (categoriesResult.isNotEmpty()) {
            categoryCache = categoriesResult
        }
        return categoriesResult
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
        // Case-insensitive contains: Firestore startAt/endAt is case-sensitive and
        // misses typical user input (e.g. "son" vs "Sơn Tùng").
        return getLatestSongs(limit = 100)
            .asSequence()
            .filter { song ->
                song.title.contains(keyword, ignoreCase = true) ||
                    song.artistText.contains(keyword, ignoreCase = true)
            }
            .take(limit.toInt().coerceAtLeast(1))
            .toList()
    }

    override suspend fun searchSingersByName(term: String, limit: Long): List<FirestoreSinger> {
        val keyword = term.trim()
        if (keyword.isEmpty()) return emptyList()
        return getSingers()
            .asSequence()
            .filter { it.name.contains(keyword, ignoreCase = true) }
            .take(limit.toInt().coerceAtLeast(1))
            .toList()
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

    /**
     * Android Firestore snapshots do not expose document createTime, so categories are listed
     * through the REST API and sorted oldest → newest.
     */
    private suspend fun fetchCategoriesOrderedByCreateTime(): List<FirestoreCategory> {
        val projectId = db.app.options.projectId?.takeIf { it.isNotBlank() } ?: return emptyList()
        val apiKey = db.app.options.apiKey.orEmpty()
        val token = runCatching {
            firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
        }.getOrNull()

        return withContext(Dispatchers.IO) {
            val documents = mutableListOf<Pair<FirestoreCategory, Instant?>>()
            var pageToken: String? = null
            do {
                val url = buildCategoriesUrl(projectId, apiKey, pageToken)
                val json = httpGetJson(url, token) ?: break
                val docs = json.optJSONArray("documents") ?: break
                for (index in 0 until docs.length()) {
                    val doc = docs.optJSONObject(index) ?: continue
                    val id = doc.optString("name").substringAfterLast('/')
                    if (id.isBlank()) continue
                    val name = doc.optJSONObject("fields")
                        ?.optJSONObject("name")
                        ?.optString("stringValue")
                        .orEmpty()
                    val createdAt = doc.optString("createTime")
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                    documents += FirestoreCategory(id = id, name = name) to createdAt
                }
                pageToken = json.optString("nextPageToken").takeIf { it.isNotBlank() }
            } while (pageToken != null)

            documents
                .sortedWith(
                    compareBy(
                        { it.second ?: Instant.MAX },
                        { it.first.name },
                    ),
                )
                .map { it.first }
        }
    }

    private fun buildCategoriesUrl(projectId: String, apiKey: String, pageToken: String?): String =
        buildString {
            append("https://firestore.googleapis.com/v1/projects/")
            append(projectId)
            append("/databases/%28default%29/documents/categories?pageSize=100")
            if (apiKey.isNotBlank()) {
                append("&key=")
                append(URLEncoder.encode(apiKey, Charsets.UTF_8.name()))
            }
            if (!pageToken.isNullOrBlank()) {
                append("&pageToken=")
                append(URLEncoder.encode(pageToken, Charsets.UTF_8.name()))
            }
        }

    private fun httpGetJson(url: String, bearerToken: String?): JSONObject? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            if (!bearerToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        } finally {
            connection.disconnect()
        }
    }
}
