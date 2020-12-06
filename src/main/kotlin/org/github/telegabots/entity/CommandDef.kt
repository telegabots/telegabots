package org.github.telegabots.entity

import org.github.telegabots.SystemCommands

data class CommandDef(val titleId: String,
                      val handler: String?,
                      val state: StateDef?) {
    fun isBackCommand(): Boolean = SystemCommands.SYS_BACK == titleId

    fun isRefreshCommand(): Boolean = SystemCommands.SYS_REFRESH == titleId
}
