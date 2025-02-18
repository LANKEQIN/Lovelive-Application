package com.lovelive.dreamycolor.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lovelive.dreamycolor.database.dao.EncyclopediaDao
import com.lovelive.dreamycolor.model.CharacterCard

@Database(
    entities = [CharacterCard::class],
    version = 2,    // 每次修改数据库结构需递增
    exportSchema = false
)
abstract class EncyclopediaDatabase : RoomDatabase() {
    abstract fun encyclopediaDao(): EncyclopediaDao

    companion object {
        @Volatile
        private var INSTANCE: EncyclopediaDatabase? = null

        // 添加同步锁保证线程安全
        fun getDatabase(context: Context): EncyclopediaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    EncyclopediaDatabase::class.java,
                    "encyclopedia.db" // 更规范的数据库名称
                ).fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
