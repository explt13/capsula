package com.capsula.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

// База данных приложения: две таблицы (capsules и capsule_events) и один DAO.
// Файл capsula.db хранится во внутренней памяти приложения, поэтому данные переживают перезапуск.
@Database(
    entities = [CapsuleEntity::class, CapsuleEventEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CapsulaDatabase : RoomDatabase() {

    abstract fun capsuleDao(): CapsuleDao

    companion object {
        private const val DATABASE_NAME = "capsula.db"

        // onFirstCreate вызывается один раз — когда файл базы создаётся впервые (первый запуск после установки).
        fun build(context: Context, onFirstCreate: () -> Unit): CapsulaDatabase =
            Room.databaseBuilder(context, CapsulaDatabase::class.java, DATABASE_NAME)
                .addCallback(
                    object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            onFirstCreate()
                        }
                    },
                )
                .build()
    }
}
