package com.radiomaster.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.radiomaster.app.R

@Database(entities = [FavoriteStationEntity::class, RecentStationEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun recentDao(): RecentDao
}
