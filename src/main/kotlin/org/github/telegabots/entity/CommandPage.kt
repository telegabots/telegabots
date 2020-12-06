package org.github.telegabots.entity

/**
 * Entity related with block and concrete command page
 */
data class CommandPage(val id: Long = 0,
                       val blockId: Long,
                       val handler: String,
                       val subCommands: List<List<CommandDef>>) {
    fun isValid(): Boolean = blockId != 0L && handler.isNotBlank()
}
