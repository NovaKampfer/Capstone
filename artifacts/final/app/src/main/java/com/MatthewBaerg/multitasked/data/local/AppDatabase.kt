package com.MatthewBaerg.multitasked.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [BoardEntity::class, TaskEntity::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun boardDao(): BoardDao
    abstract fun taskDao(): TaskDao

    companion object {
        const val FILE_NAME = "multitasked-local.db"

        // v1 is the documented initial local schema used by the upgrade test.
        // This upgrades local SQLite only; it does not import Firestore documents.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_tasks ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE local_tasks ADD COLUMN dueDate INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE local_tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'NONE'")
            }
        }

        fun create(context: Context, name: String = FILE_NAME): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .build() // No main-thread access and no destructive migration fallback.
    }
}
