package co.adityarajput.fileflow.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.adityarajput.fileflow.data.Verb

@Entity(tableName = "executions")
data class Execution(
    val fileName: String,

    @ColumnInfo(defaultValue = "MOVE")
    val verb: Verb,

    val timestamp: Long = System.currentTimeMillis(),

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)
