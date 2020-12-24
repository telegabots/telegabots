package org.github.telegabots.entity

import org.github.telegabots.SystemCommands

data class CommandDef(val titleId: String,
                      val handler: String?,
                      val state: StateDef?) {
    fun isBackCommand(): Boolean = SystemCommands.BACK == titleId

    fun isRefreshCommand(): Boolean = SystemCommands.REFRESH == titleId
}
