package co.adityarajput.fileflow.data.daos

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import co.adityarajput.fileflow.data.models.Server
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Upsert
    suspend fun upsert(vararg servers: Server)

    @Query("SELECT * from servers ORDER BY id ASC")
    fun list(): Flow<List<Server>>

    @Delete
    suspend fun delete(group: Server)
}
