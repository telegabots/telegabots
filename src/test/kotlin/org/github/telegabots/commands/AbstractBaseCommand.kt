package org.github.telegabots.commands

import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.BaseCommand
import org.github.telegabots.api.annotation.TextHandler

internal abstract class AbstractBaseCommand : BaseCommand() {
    @TextHandler
    fun handle(msg: String) {
        CODE_NOT_REACHED()
    }
}
