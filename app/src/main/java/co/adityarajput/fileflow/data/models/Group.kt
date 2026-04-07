package co.adityarajput.fileflow.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "groups")
data class Group(
    val name: String,

    val ruleIds: List<Int>,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {
    fun shortcutId() = hashCode().toString()
}

val ALL_RULES = Group("all", emptyList(), -420)
