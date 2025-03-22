package org.github.telegabots.api

/**
 * All system commands ids
 */
object SystemCommands {
    /**
     * Removes current page and send _REFRESH to previous one
     */
    const val GO_BACK = "_GO_BACK"

    /**
     * Refresh content of current page
     */
    const val REFRESH = "_REFRESH"

    /**
     * Do nothing
     */
    const val NOTHING = "_NOTHING"

    @JvmField
    val ALL = listOf(GO_BACK, REFRESH, NOTHING)
}
