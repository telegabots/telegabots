package org.github.telegabots.entity

import java.time.LocalDateTime

/**
 * Entity related with block and concrete command page
 */
data class CommandPage(
    val id: Long = 0,

    /**
     * Related block id
     */
    val blockId: Long,

    /**
     * Handler of the current page
     */
    val handler: String,

    /**
     * Command definitions under message
     */
    val commandDefs: List<List<CommandDef>>,

    /**
     * Time when page created
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Time when page was updated
     */
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    fun isValid(): Boolean = blockId != 0L && handler.isNotBlank()
}
