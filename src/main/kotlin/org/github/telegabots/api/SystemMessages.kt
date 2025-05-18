package org.github.telegabots.api

/**
 * All system messages
 */
object SystemMessages {
    /**
     * Removes current page and send _REFRESH to the previous one
     */
    const val GO_BACK = "_GO_BACK"

    /**
     * Refreshes content of current page
     */
    const val REFRESH = "_REFRESH"

    /**
     * Does nothing
     */
    const val NOTHING = "_NOTHING"

    @JvmField
    val ALL = listOf(GO_BACK, REFRESH, NOTHING)
}
