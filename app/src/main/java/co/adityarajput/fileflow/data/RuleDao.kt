package co.adityarajput.fileflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import co.adityarajput.fileflow.data.models.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Upsert
    suspend fun upsert(vararg rules: Rule)

    @Query("SELECT * from rules ORDER BY id ASC")
    fun list(): Flow<List<Rule>>

    @Query("UPDATE rules SET executions = executions + 1 WHERE id = :id")
    suspend fun registerExecution(id: Int)

    @Query("UPDATE rules SET enabled = 1 - enabled WHERE id = :id")
    suspend fun toggle(id: Int)

    @Delete
    suspend fun delete(rule: Rule)
}
