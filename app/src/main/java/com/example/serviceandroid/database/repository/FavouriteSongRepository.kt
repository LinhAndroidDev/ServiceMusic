package com.example.serviceandroid.database.repository

import com.example.serviceandroid.database.SongEntity
import com.example.serviceandroid.database.dao.FavouriteSongDao
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.DateUtils
import com.example.serviceandroid.utils.ExtensionFunctions.toSong
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

enum class ArrangeMusic {
    NEWEST,
    OLDEST,
    BY_NAME_SONG,
    BY_NAME_SINGLE
}

@Singleton
class FavouriteSongRepository @Inject constructor(private val dao: FavouriteSongDao) {

    private val dateFormat = SimpleDateFormat(DateUtils.TIME, Locale.getDefault())

    fun observeFavouriteCount(): Flow<Int> = dao.observeFavouriteCount()

    fun observeAllEntities(): Flow<List<SongEntity>> = dao.observeAllEntities()

    suspend fun getAll(typeArrangement: ArrangeMusic = ArrangeMusic.NEWEST): MutableList<Song>? {
        val raw = dao.getAll() ?: return null
        return entitiesToSortedSongs(raw, typeArrangement)
    }

    fun entitiesToSortedSongs(entities: List<SongEntity>, typeArrangement: ArrangeMusic): MutableList<Song> {
        return sortEntities(entities, typeArrangement).map { it.toSong() }.toMutableList()
    }

    private fun sortEntities(entities: List<SongEntity>, typeArrangement: ArrangeMusic): List<SongEntity> {
        if (entities.isEmpty()) return emptyList()
        return when (typeArrangement) {
            ArrangeMusic.NEWEST -> entities.sortedByDescending {
                it.timeCreate?.let { timeCreate -> dateFormat.parse(timeCreate) }
            }

            ArrangeMusic.OLDEST -> entities.sortedBy {
                it.timeCreate?.let { timeCreate -> dateFormat.parse(timeCreate) }
            }

            ArrangeMusic.BY_NAME_SONG -> {
                val collator = Collator.getInstance(Locale("vi", "VN"))
                collator.strength = Collator.PRIMARY
                val names = entities.map { it.title }.sortedWith(collator)
                val sortedList = mutableListOf<SongEntity>()
                if (names.isNotEmpty()) {
                    for (i in names.indices) {
                        for (j in entities.indices) {
                            if (entities[j].title == names[i]) {
                                sortedList.add(entities[j])
                            }
                        }
                    }
                }
                sortedList
            }

            ArrangeMusic.BY_NAME_SINGLE -> {
                val names = entities.map { it.nameSinger }.sorted()
                val sortedList = mutableListOf<SongEntity>()
                if (names.isNotEmpty()) {
                    for (i in names.indices) {
                        for (j in entities.indices) {
                            if (entities[j].nameSinger == names[i]) {
                                sortedList.add(entities[j])
                            }
                        }
                    }
                }
                sortedList
            }
        }
    }

    suspend fun insertSong(song: Song, timeCreate: String) {
        dao.insertSong(SongEntity(song, timeCreate))
    }

    suspend fun deleteSongById(id: Int) = dao.deleteSongById(id)

    suspend fun checkSongById(id: Int): Boolean {
        return dao.checkSongById(id) != null
    }
}