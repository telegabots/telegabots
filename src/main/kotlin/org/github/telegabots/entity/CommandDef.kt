package org.github.telegabots.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import org.github.telegabots.api.CommandBehaviour
import org.github.telegabots.api.SystemCommands

data class CommandDef(val titleId: String,
                      val title: String,
                      val handler: String?,
                      val behaviour: CommandBehaviour?,
                      val state: StateDef?) {
    @JsonIgnore
    fun isBackCommand(): Boolean = SystemCommands.GO_BACK == titleId

    @JsonIgnore
    fun isRefreshCommand(): Boolean = SystemCommands.REFRESH == titleId

    @JsonIgnore
    fun isNothingCommand(): Boolean = SystemCommands.NOTHING == titleId
}
