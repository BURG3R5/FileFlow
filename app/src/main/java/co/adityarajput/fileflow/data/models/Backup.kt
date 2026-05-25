package co.adityarajput.fileflow.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Backup(
    val rules: List<Rule>,
    val groups: List<Group>,
    val servers: List<Server>,
)
