package org.github.telegabots.api.entity

import org.github.telegabots.api.SystemCommands
import org.github.telegabots.entity.StateDef

data class CommandDef(val titleId: String,
                      val handler: String?,
                      val state: StateDef?) {
    fun isBackCommand(): Boolean = SystemCommands.GO_BACK == titleId

    fun isRefreshCommand(): Boolean = SystemCommands.REFRESH == titleId
}
