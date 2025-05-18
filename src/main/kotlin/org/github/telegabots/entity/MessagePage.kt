package org.github.telegabots.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.LocalDateTime

/**
 * Database entity related with block and concrete message page
 */
data class MessagePage(
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
     * Button definitions of page buttons
     */
    val buttonDefs: List<List<ButtonDef>> = emptyList(),

    /**
     * Time when page created
     */
    val createdAt: LocalDateTime = LocalDateTime.now(),

    /**
     * Time when page was updated
     */
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @JsonIgnore
    fun isValid(): Boolean = blockId > 0L && handler.isNotBlank()
}
