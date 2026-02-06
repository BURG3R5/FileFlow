package co.adityarajput.fileflow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Rule

@Database([Rule::class, Execution::class], version = 1)
@TypeConverters(Converters::class)
abstract class FileFlowDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun executionDao(): ExecutionDao

    companion object {
        @Volatile
        private var instance: FileFlowDatabase? = null

        fun getDatabase(context: Context): FileFlowDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(context, FileFlowDatabase::class.java, "fileflow_database")
                    .build().also { instance = it }
            }
        }
    }
}
