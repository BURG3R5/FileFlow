package co.adityarajput.fileflow.data

import android.content.Context
import androidx.room.*
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Rule

@Database([Rule::class, Execution::class], version = 2, autoMigrations = [AutoMigration(1, 2)])
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
