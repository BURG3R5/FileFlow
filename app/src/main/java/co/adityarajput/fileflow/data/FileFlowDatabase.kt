package co.adityarajput.fileflow.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.AutoMigrationSpec
import co.adityarajput.fileflow.data.daos.ExecutionDao
import co.adityarajput.fileflow.data.daos.GroupDao
import co.adityarajput.fileflow.data.daos.RuleDao
import co.adityarajput.fileflow.data.daos.ServerDao
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Group
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.data.models.Server

@Database(
    [Rule::class, Execution::class, Group::class, Server::class],
    version = 7,
    autoMigrations = [
        AutoMigration(1, 2),
        AutoMigration(2, 3, FileFlowDatabase.DeleteEColumnAV::class),
        AutoMigration(3, 4),
        AutoMigration(4, 5),
        AutoMigration(5, 6),
        AutoMigration(6, 7),
    ],
)
@TypeConverters(Converters::class)
abstract class FileFlowDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun executionDao(): ExecutionDao
    abstract fun groupDao(): GroupDao
    abstract fun serverDao(): ServerDao

    @DeleteColumn("executions", "actionVerb")
    class DeleteEColumnAV : AutoMigrationSpec

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
