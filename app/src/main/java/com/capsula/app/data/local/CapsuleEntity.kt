package com.capsula.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsula.app.data.model.Mood

// Таблица capsules. Одна строка — одна капсула на любом этапе жизненного цикла (черновик, запечатана, вскрыта).
@Entity(
    tableName = "capsules",
    indices = [Index(value = ["trashed_at"])],
)
data class CapsuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    @ColumnInfo(name = "question_for_future")
    val questionForFuture: String?,
    @ColumnInfo(name = "mood_at_sealing")
    val moodAtSealing: Mood?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "sealed_at")
    val sealedAt: Long?,
    @ColumnInfo(name = "unlock_at")
    val unlockAt: Long?,
    @ColumnInfo(name = "opened_at")
    val openedAt: Long?,
    @ColumnInfo(name = "mood_at_opening")
    val moodAtOpening: Mood?,
    val reflection: String?,
    @ColumnInfo(name = "postpone_count")
    val postponeCount: Int,
    @ColumnInfo(name = "trashed_at")
    val trashedAt: Long?,
)
