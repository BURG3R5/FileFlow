package co.adityarajput.fileflow.data

import androidx.room.TypeConverter
import co.adityarajput.fileflow.data.models.Action
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter
    fun fromAction(action: Action) = Json.encodeToString(action)

    @TypeConverter
    fun toAction(value: String) = try {
        Json.decodeFromString<Action>(value)
    } catch (_: Exception) {
        Action.MOVE("", "", "", "")
    }
}
