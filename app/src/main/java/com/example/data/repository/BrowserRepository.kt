package com.example.data.repository

import com.example.data.dao.BrowserDao
import com.example.data.entity.Bookmark
import com.example.data.entity.HistoryEntry
import kotlinx.coroutines.flow.Flow

class BrowserRepository(private val browserDao: BrowserDao) {
    val allBookmarks: Flow<List<Bookmark>> = browserDao.getAllBookmarks()
    val recentHistory: Flow<List<HistoryEntry>> = browserDao.getRecentHistory()

    suspend fun addBookmark(title: String, url: String) {
        browserDao.insertBookmark(Bookmark(title = title, url = url))
    }

    suspend fun deleteBookmark(bookmark: Bookmark) {
        browserDao.deleteBookmark(bookmark)
    }

    suspend fun deleteBookmarkByUrl(url: String) {
        browserDao.deleteBookmarkByUrl(url)
    }

    fun isBookmarked(url: String): Flow<Boolean> = browserDao.isBookmarked(url)

    suspend fun addHistory(title: String, url: String) {
        browserDao.insertHistory(HistoryEntry(title = title, url = url))
    }

    suspend fun deleteHistory(id: Long) {
        browserDao.deleteHistoryById(id)
    }

    suspend fun clearHistory() {
        browserDao.clearHistory()
    }
}
