package co.adityarajput.fileflow.data

import co.adityarajput.fileflow.data.daos.ExecutionDao
import co.adityarajput.fileflow.data.daos.GroupDao
import co.adityarajput.fileflow.data.daos.RuleDao
import co.adityarajput.fileflow.data.daos.ServerDao
import co.adityarajput.fileflow.data.models.*
import co.adityarajput.fileflow.utils.Logger
import kotlinx.coroutines.flow.first

class Repository(
    private val ruleDao: RuleDao,
    private val executionDao: ExecutionDao,
    private val groupDao: GroupDao,
    private val serverDao: ServerDao,
) {
    suspend fun upsert(vararg rules: Rule) = ruleDao.upsert(*rules)

    suspend fun upsert(vararg executions: Execution) = executionDao.upsert(*executions)

    suspend fun upsert(vararg groups: Group) = groupDao.upsert(*groups)

    suspend fun upsert(vararg servers: Server) = serverDao.upsert(*servers)

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

    fun servers() = serverDao.list()

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

    suspend fun delete(server: Server) = serverDao.delete(server)
}
