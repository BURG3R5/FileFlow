package co.adityarajput.fileflow.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "rules")
data class Rule(
    val action: Action,

    val enabled: Boolean = true,

    val executions: Int = 0,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)
