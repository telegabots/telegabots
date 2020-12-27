package org.github.telegabots.api.entity

/**
 * Entity related with block and concrete command page
 */
data class CommandPage(val id: Long = 0,
                       val blockId: Long,
                       val handler: String,
                       val commandDefs: List<List<CommandDef>>) {
    fun isValid(): Boolean = blockId != 0L && handler.isNotBlank()
}
