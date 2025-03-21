package org.github.telegabots.api

enum class CommandBehaviour {
    /**
     * Create separate page and separate state for command
     */
    SeparatePage,

    /**
     * Use parent page without parent local state
     */
    ParentPage,

    /**
     * Use parent page and parent local state
     */
    ParentPageState,
}
