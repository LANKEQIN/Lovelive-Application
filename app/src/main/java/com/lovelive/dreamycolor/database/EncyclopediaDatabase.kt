package com.lovelive.dreamycolor.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lovelive.dreamycolor.database.dao.EncyclopediaDao
import com.lovelive.dreamycolor.model.CharacterCard
import com.lovelive.dreamycolor.model.VoiceActorCard
import com.lovelive.dreamycolor.database.dao.VoiceActorDao
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CharacterCard::class, VoiceActorCard::class],
    version = 4,
    exportSchema = true
)
abstract class EncyclopediaDatabase : RoomDatabase() {
    abstract fun encyclopediaDao(): EncyclopediaDao
    abstract fun voiceActorDao(): VoiceActorDao

    companion object {
        @Volatile
        private var INSTANCE: EncyclopediaDatabase? = null

        fun getDatabase(context: Context): EncyclopediaDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    EncyclopediaDatabase::class.java,
                    "encyclopedia.db"
                )
                    .addMigrations(MIGRATION_1_2,MIGRATION_2_3,MIGRATION_3_4)
                    .fallbackToDestructiveMigration() // 添加这行代码允许破坏性迁移
                    .build()
                    .also { INSTANCE = it }
            }
        }

        // 数据库迁移定义
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 如果表结构有改变，则在这里写对应的 ALTER TABLE 语句；
                // 如果仅仅是版本号升级而表结构未变，这里可以留空（或写注释说明）
            }
        }
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 如果表结构有改变，则在这里写对应的 ALTER TABLE 语句；
                // 如果仅仅是版本号升级而表结构未变，这里可以留空（或写注释说明）
            }
        }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 如果表结构有改变，则在这里写对应的 ALTER TABLE 语句；
                // 如果仅仅是版本号升级而表结构未变，这里可以留空（或写注释说明）
            }
        }
    }
}
