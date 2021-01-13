package org.github.telegabots.entity

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
     * Message id of the telegram message. Used for text messages
     */
    val messageId: Int?,

    /**
     * Command definitions under message
     */
    val commandDefs: List<List<CommandDef>>
) {
    fun isValid(): Boolean = blockId != 0L && handler.isNotBlank()
}
