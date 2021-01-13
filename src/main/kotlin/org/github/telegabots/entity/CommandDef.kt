package org.github.telegabots.entity

import org.github.telegabots.api.CommandBehaviour
import org.github.telegabots.api.SystemCommands

data class CommandDef(val titleId: String,
                      val title: String,
                      val handler: String?,
                      val behaviour: CommandBehaviour?,
                      val state: StateDef?) {
    fun isBackCommand(): Boolean = SystemCommands.GO_BACK == titleId

    fun isRefreshCommand(): Boolean = SystemCommands.REFRESH == titleId
}
