package co.adityarajput.fileflow.data

import co.adityarajput.fileflow.data.models.ALL_RULES
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Group
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.Logger
import kotlinx.coroutines.flow.first

class Repository(
    private val ruleDao: RuleDao,
    private val executionDao: ExecutionDao,
    private val groupDao: GroupDao,
) {
    suspend fun upsert(vararg rules: Rule) = ruleDao.upsert(*rules)

    suspend fun upsert(vararg executions: Execution) = executionDao.upsert(*executions)

    suspend fun upsert(vararg groups: Group) = groupDao.upsert(*groups)

    fun rules() = ruleDao.list()

    fun rule(id: Int) = ruleDao.get(id)

    fun executions() = executionDao.list()

    fun groups() = groupDao.list()

    suspend fun group(id: Int): Pair<Group?, List<Rule>> {
        val rules = ruleDao.list().first()

        if (id == ALL_RULES.id)
            return ALL_RULES to rules

        val group = groupDao.get(id) ?: return null to emptyList()

        return group to
                (if (group.ruleIds.isEmpty()) rules
                else rules.filter { it.id in group.ruleIds })
    }

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

    suspend fun delete(group: Group) = groupDao.delete(group)
}
