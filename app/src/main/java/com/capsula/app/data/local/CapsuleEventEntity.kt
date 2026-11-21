package com.capsula.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.capsula.app.data.model.CapsuleEventType

// Таблица capsule_events: история капсулы. Связь «одна капсула — много событий» через внешний ключ.
// CASCADE: при окончательном удалении капсулы её история удаляется вместе с ней.
@Entity(
    tableName = "capsule_events",
    foreignKeys = [
        ForeignKey(
            entity = CapsuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["capsule_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["capsule_id"])],
)
data class CapsuleEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "capsule_id")
    val capsuleId: Long,
    val type: CapsuleEventType,
    val timestamp: Long,
    val details: String?,
)

// Результат JOIN-запроса: событие + название капсулы и признак «в корзине».
data class ActivityRow(
    @Embedded
    val event: CapsuleEventEntity,
    @ColumnInfo(name = "capsule_title")
    val capsuleTitle: String,
    @ColumnInfo(name = "capsule_trashed_at")
    val capsuleTrashedAt: Long?,
)
