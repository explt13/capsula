package com.capsula.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object): все SQL-запросы приложения.
 *
 * - Запросы, возвращающие Flow, «живые»: Room сам отправляет новый список, когда таблица меняется.
 *   Поэтому изменение на одном экране сразу видно на всех остальных.
 * - suspend-функции выполняются Room в фоновом потоке и не блокируют UI.
 * - Методы с @Transaction меняют капсулу и пишут событие в историю атомарно: либо оба изменения, либо ни одного.
 */
@Dao
abstract class CapsuleDao {

    @Query("SELECT * FROM capsules WHERE trashed_at IS NULL ORDER BY created_at DESC")
    abstract fun observeActive(): Flow<List<CapsuleEntity>>

    @Query("SELECT * FROM capsules WHERE trashed_at IS NOT NULL ORDER BY trashed_at DESC")
    abstract fun observeTrashed(): Flow<List<CapsuleEntity>>

    @Query("SELECT * FROM capsules WHERE id = :id")
    abstract fun observeById(id: Long): Flow<CapsuleEntity?>

    @Query("SELECT * FROM capsules WHERE id = :id")
    abstract suspend fun getById(id: Long): CapsuleEntity?

    @Query("SELECT * FROM capsule_events WHERE capsule_id = :capsuleId ORDER BY timestamp ASC, id ASC")
    abstract fun observeEvents(capsuleId: Long): Flow<List<CapsuleEventEntity>>

    @Query(
        """
        SELECT e.*, c.title AS capsule_title, c.trashed_at AS capsule_trashed_at
        FROM capsule_events AS e
        INNER JOIN capsules AS c ON c.id = e.capsule_id
        ORDER BY e.timestamp DESC, e.id DESC
        LIMIT :limit
        """,
    )
    abstract fun observeRecentActivity(limit: Int): Flow<List<ActivityRow>>

    @Insert
    abstract suspend fun insertCapsule(capsule: CapsuleEntity): Long

    @Update
    abstract suspend fun updateCapsule(capsule: CapsuleEntity)

    @Insert
    abstract suspend fun insertEvent(event: CapsuleEventEntity): Long

    @Query("DELETE FROM capsules WHERE id = :id")
    abstract suspend fun deleteCapsule(id: Long)

    @Query("DELETE FROM capsules WHERE trashed_at IS NOT NULL")
    abstract suspend fun deleteAllTrashed(): Int

    // Новая капсула + первое событие истории в одной транзакции. Возвращает id новой капсулы.
    @Transaction
    open suspend fun insertWithEvent(capsule: CapsuleEntity, event: CapsuleEventEntity): Long {
        val id = insertCapsule(capsule)
        insertEvent(event.copy(capsuleId = id))
        return id
    }

    // Изменение капсулы + запись в историю в одной транзакции.
    @Transaction
    open suspend fun updateWithEvent(capsule: CapsuleEntity, event: CapsuleEventEntity) {
        updateCapsule(capsule)
        insertEvent(event)
    }
}
