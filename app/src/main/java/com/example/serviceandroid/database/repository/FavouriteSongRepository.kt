package com.example.serviceandroid.database.repository

import com.example.serviceandroid.database.SongEntity
import com.example.serviceandroid.database.dao.FavouriteSongDao
import com.example.serviceandroid.model.Song
import com.example.serviceandroid.utils.DateUtils
import com.example.serviceandroid.utils.ExtensionFunctions.toSong
import dagger.hilt.android.scopes.ViewModelScoped
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

enum class ArrangeMusic {
    NEWEST,
    OLDEST,
    BY_NAME_SONG,
    BY_NAME_SINGLE
}

@ViewModelScoped
class FavouriteSongRepository @Inject constructor(private val dao: FavouriteSongDao) {
    suspend fun getAll(typeArrangement: ArrangeMusic = ArrangeMusic.NEWEST): MutableList<Song>? {
        val dateFormat = SimpleDateFormat(DateUtils.TIME, Locale.getDefault())
        val sortedList = when (typeArrangement) {
            ArrangeMusic.NEWEST -> {
                dao.getAll()?.sortedByDescending {
                    it.timeCreate?.let { timeCreate -> dateFormat.parse(timeCreate) }
                }?.toMutableList()
            }

            ArrangeMusic.OLDEST -> {
                dao.getAll()?.sortedBy {
                    it.timeCreate?.let { timeCreate -> dateFormat.parse(timeCreate) }
                }?.toMutableList()
            }

            ArrangeMusic.BY_NAME_SONG -> {
                val musics = dao.getAll()?.map { it.title }
                val collator = Collator.getInstance(Locale("vi", "VN"))
                collator.strength = Collator.PRIMARY
                val names = musics?.sortedWith(collator)
                val sortedList = mutableListOf <SongEntity>()
                dao.getAll()?.let { list ->
                    if(names?.isNotEmpty() == true) {
                        for(i in names.indices) {
                            for (j in list.indices) {
                                if(list[j].title == names[i]) {
                                    sortedList.add(list[j])
                                }
                            }
                        }
                    }
                }
                sortedList
            }

            ArrangeMusic.BY_NAME_SINGLE -> {
                val names = dao.getAll()?.map { it.nameSinger }?.sorted()
                val sortedList = mutableListOf <SongEntity>()
                dao.getAll()?.let { list ->
                    if(names?.isNotEmpty() == true) {
                        for(i in names.indices) {
                            for (j in list.indices) {
                                if(list[j].nameSinger == names[i]) {
                                    sortedList.add(list[j])
                                }
                            }
                        }
                    }
                }
                sortedList
            }
        }

        return sortedList?.map { songEntity ->
            songEntity.toSong()
        }?.toMutableList()
    }

    suspend fun insertSong(song: Song, timeCreate: String) {
        dao.insertSong(SongEntity(song, timeCreate))
    }

    suspend fun deleteSongById(id: Int) = dao.deleteSongById(id)

    suspend fun checkSongById(id: Int): Boolean {
        return dao.checkSongById(id) != null
    }
}