package co.adityarajput.fileflow.data

import androidx.room.TypeConverter
import co.adityarajput.fileflow.data.models.Action
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class Converters {
    @TypeConverter
    fun fromAction(action: Action) = Json.encodeToString(action)

    @TypeConverter
    fun toAction(value: String) = try {
        Json.decodeFromString<Action>(value)
    } catch (_: Exception) {
        try {
            Json.decodeFromString<Action>(
                Json.encodeToString(
                    Json.parseToJsonElement(value)
                        .jsonObject.toMap()
                        .filterKeys { it != "title" },
                ),
            )
        } catch (_: Exception) {
            Action.entries[0]
        }
    }

    @TypeConverter
    fun fromIntList(list: List<Int>) = list.joinToString(",")

    @TypeConverter
    fun toIntList(value: String) = value.split(",").mapNotNull { it.toIntOrNull() }
}
