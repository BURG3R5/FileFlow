package co.adityarajput.fileflow.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import co.adityarajput.fileflow.data.models.Execution
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionDao {
    @Upsert
    suspend fun upsert(vararg execution: Execution)

    @Query("SELECT * FROM executions ORDER BY id DESC")
    fun list(): Flow<List<Execution>>

    @Query("SELECT COUNT(*) FROM executions")
    suspend fun count(): Int

    @Query("DELETE FROM executions WHERE id IN (SELECT id FROM executions ORDER BY timestamp ASC LIMIT :count)")
    suspend fun trim(count: Int)
}
