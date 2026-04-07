package co.adityarajput.fileflow.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import co.adityarajput.fileflow.data.models.Group
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Upsert
    suspend fun upsert(vararg groups: Group)

    @Query("SELECT * from `groups` ORDER BY id ASC")
    fun list(): Flow<List<Group>>

    @Query("SELECT * from `groups` WHERE id = :id")
    fun get(id: Int): Group?

    @Delete
    suspend fun delete(group: Group)
}
