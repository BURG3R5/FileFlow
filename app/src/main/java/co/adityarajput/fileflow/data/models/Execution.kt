package co.adityarajput.fileflow.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "executions")
data class Execution(
    val fileName: String,

    val actionVerb: Int,

    val timestamp: Long = System.currentTimeMillis(),

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)
