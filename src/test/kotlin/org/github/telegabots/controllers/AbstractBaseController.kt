package org.github.telegabots.controllers

import org.github.telegabots.CODE_NOT_REACHED
import org.github.telegabots.api.BaseController
import org.github.telegabots.api.annotation.TextHandler

internal abstract class AbstractBaseController : BaseController() {
    @TextHandler
    fun handle(msg: String) {
        CODE_NOT_REACHED()
    }
}
