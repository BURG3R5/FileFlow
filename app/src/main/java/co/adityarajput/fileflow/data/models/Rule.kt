package co.adityarajput.fileflow.data.models

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.utils.toAccurateHumanReadableTime
import co.adityarajput.fileflow.views.dullStyle
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "rules")
data class Rule(
    val action: Action,

    val enabled: Boolean = true,

    val executions: Int = 0,

    @ColumnInfo(defaultValue = "3600000")
    val interval: Long? = Constants.ONE_HOUR_IN_MILLIS,

    @ColumnInfo(defaultValue = "NULL")
    val cronString: String? = null,

    @ColumnInfo(defaultValue = "NULL")
    val name: String? = null,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
) {
    @Composable
    fun getDescription() = buildAnnotatedString {
        if (name != null)
            append(action.srcFileNamePattern + "\n")

        append(action.getDescription())

        if (interval != null) {
            withStyle(dullStyle) { append("\nevery ") }
            append(interval.toAccurateHumanReadableTime())
        }
        if (cronString != null) {
            withStyle(dullStyle) { append("\nwhen ") }
            append(cronString)
        }
    }
}
