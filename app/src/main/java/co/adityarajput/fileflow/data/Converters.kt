package co.adityarajput.fileflow.data

import androidx.room.TypeConverter
import co.adityarajput.fileflow.data.models.Action

class Converters {
    @TypeConverter
    fun fromAction(action: Action) = action.toString()

    @TypeConverter
    fun toAction(value: String) = Action.fromString(value)
}
