package co.adityarajput.fileflow.data

import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.Logger

class Repository(
    private val ruleDao: RuleDao,
    private val executionDao: ExecutionDao,
) {
    suspend fun upsert(vararg rules: Rule) = ruleDao.upsert(*rules)

    suspend fun upsert(vararg executions: Execution) = executionDao.upsert(*executions)

    fun rules() = ruleDao.list()

    fun executions() = executionDao.list()

    suspend fun registerExecution(rule: Rule, execution: Execution) {
        ruleDao.registerExecution(rule.id)
        executionDao.upsert(execution)

        val count = executionDao.count()
        if (count > 50) {
            Logger.d("Repository", "Deleting oldest ${count - 50} execution(s)")
            executionDao.trim(count - 50)
        }
    }

    suspend fun toggle(rule: Rule) = ruleDao.toggle(rule.id)

    suspend fun delete(rule: Rule) = ruleDao.delete(rule)
}
