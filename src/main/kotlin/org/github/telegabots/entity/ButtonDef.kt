package org.github.telegabots.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.github.telegabots.api.SystemMessages

/**
 * Database entity for button definition
 */
data class ButtonDef(val titleId: String,
                     val title: String,
                     val handler: String?,
                     val state: StateDef?) {
    @JsonIgnore
    fun isBackMessage(): Boolean = SystemMessages.GO_BACK == titleId

    @JsonIgnore
    fun isRefreshMessage(): Boolean = SystemMessages.REFRESH == titleId

    @JsonIgnore
    fun isNothingMessage(): Boolean = SystemMessages.NOTHING == titleId
}
