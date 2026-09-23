package com.example.serviceandroid.data.search

import com.example.serviceandroid.data.auth.AuthRepository
import com.example.serviceandroid.utils.VietnameseFold
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

interface SearchHistoryRepository {
    fun observeRecentQueries(limit: Int = SearchCatalog.HISTORY_LIMIT): Flow<List<SearchQuery>>
    suspend fun recordQuery(raw: String)
    suspend fun deleteQuery(normalizedQuery: String)
    suspend fun clearAll()
}

@Singleton
class SearchHistoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
) : SearchHistoryRepository {

    override fun observeRecentQueries(limit: Int): Flow<List<SearchQuery>> =
        authRepository.authState.flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else observeRemote(user.uid, limit)
        }

    override suspend fun recordQuery(raw: String) {
        val query = raw.trim()
        val normalizedQuery = VietnameseFold.fold(query)
        val userId = authRepository.currentUser()?.uid
        if (userId == null || query.isBlank() || normalizedQuery.isBlank()) return

        queriesCollection(userId)
            .document(searchQueryDocumentId(normalizedQuery))
            .set(
                mapOf(
                    FIELD_QUERY to query,
                    FIELD_NORMALIZED_QUERY to normalizedQuery,
                    FIELD_LAST_SEARCHED_AT to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
            .await()
    }

    override suspend fun deleteQuery(normalizedQuery: String) {
        val userId = authRepository.currentUser()?.uid
        if (userId == null || normalizedQuery.isBlank()) return
        queriesCollection(userId)
            .document(searchQueryDocumentId(normalizedQuery))
            .delete()
            .await()
    }

    override suspend fun clearAll() {
        val userId = authRepository.currentUser()?.uid ?: return
        val snapshot = queriesCollection(userId).get().await()
        if (snapshot.isEmpty) return
        val batch = firestore.batch()
        snapshot.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    private fun observeRemote(userId: String, limit: Int): Flow<List<SearchQuery>> = callbackFlow {
        val registration = queriesCollection(userId)
            .orderBy(FIELD_LAST_SEARCHED_AT, Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents.orEmpty().mapNotNull(::toSearchQuery))
            }
        awaitClose { registration.remove() }
    }

    private fun queriesCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(SEARCH_QUERIES_COLLECTION)

    private fun toSearchQuery(document: DocumentSnapshot): SearchQuery? {
        val query = document.getString(FIELD_QUERY).orEmpty().ifBlank {
            document.getString(FIELD_NORMALIZED_QUERY).orEmpty()
        }
        val normalizedQuery = document.getString(FIELD_NORMALIZED_QUERY)
            .orEmpty()
            .ifBlank { VietnameseFold.fold(query) }
        if (query.isBlank() || normalizedQuery.isBlank()) return null
        return SearchQuery(
            query = query,
            normalizedQuery = normalizedQuery,
            lastSearchedAt = document.getTimestamp(FIELD_LAST_SEARCHED_AT)
                ?.toDate()
                ?.time
                ?: 0L,
        )
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val SEARCH_QUERIES_COLLECTION = "searchQueries"
        const val FIELD_QUERY = "query"
        const val FIELD_NORMALIZED_QUERY = "normalizedQuery"
        const val FIELD_LAST_SEARCHED_AT = "lastSearchedAt"
    }
}

fun searchQueryDocumentId(normalizedQuery: String): String =
    normalizedQuery.replace('/', '_').trim().take(150).ifBlank { "query" }
