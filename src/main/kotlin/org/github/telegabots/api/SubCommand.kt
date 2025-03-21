package org.github.telegabots.api

import java.util.regex.Pattern

/**
 * Data related with single button
 */
data class SubCommand(
    val titleId: String,
    val title: String? = null,
    val handler: Class<out BaseCommand>? = null,
    val behaviour: CommandBehaviour = CommandBehaviour.SeparatePage,
    val state: StateRef? = null
) {
    fun isSystemCommand() = this == REFRESH || this == GO_BACK

    /**
     * Creates new SubCommand with new state
     */
    fun withState(state: StateRef?): SubCommand =
        this.copy(state = state)

    companion object {
        inline fun <reified T : BaseCommand> of(
            state: StateRef? = null,
            titleId: String = "",
            title: String? = null,
            behaviour: CommandBehaviour = CommandBehaviour.SeparatePage
        ) =
            of(T::class.java, state, titleId, title, behaviour)

        @JvmStatic
        fun of(
            titleId: String,
            title: String? = null,
            state: StateRef? = null,
            behaviour: CommandBehaviour = CommandBehaviour.SeparatePage
        ): SubCommand = SubCommand(
            titleId = titleId,
            title = title,
            behaviour = behaviour,
            state = state
        )

        @JvmStatic
        fun of(titleId: String): SubCommand = of(titleId, null)

        @JvmStatic
        fun of(titleId: String, title: String): SubCommand = of(titleId, title, null, CommandBehaviour.SeparatePage)

        @JvmStatic
        fun of(titleId: String, title: String, state: StateRef): SubCommand =
            of(titleId, title, state, CommandBehaviour.SeparatePage)

        @JvmStatic
        fun of(titleId: String, title: String, behaviour: CommandBehaviour): SubCommand =
            of(titleId, title, null, behaviour)

        @JvmStatic
        fun of(handler: Class<out BaseCommand>): SubCommand = of(handler, null)

        @JvmStatic
        fun of(handler: Class<out BaseCommand>, state: StateRef): SubCommand = of(handler, state, "")

        @JvmStatic
        fun of(handler: Class<out BaseCommand>, state: StateRef, titleId: String): SubCommand =
            of(handler, state, titleId, null)

        @JvmStatic
        fun of(handler: Class<out BaseCommand>, state: StateRef, titleId: String, title: String): SubCommand =
            of(handler, state, titleId, title, CommandBehaviour.SeparatePage)

        @JvmStatic
        fun of(handler: Class<out BaseCommand>, titleId: String, title: String): SubCommand =
            of(handler, null, titleId, title, CommandBehaviour.SeparatePage)

        @JvmStatic
        fun of(handler: Class<out BaseCommand>, titleId: String): SubCommand =
            of(handler, null, titleId, null, CommandBehaviour.SeparatePage)

        @JvmStatic
        fun of(
            handler: Class<out BaseCommand>,
            state: StateRef? = null,
            titleId: String = "",
            title: String? = null,
            behaviour: CommandBehaviour = CommandBehaviour.SeparatePage
        ): SubCommand = SubCommand(
            titleId = if (titleId.isNotBlank()) titleId else titleIdOf(handler),
            handler = handler,
            state = state,
            title = title,
            behaviour = behaviour
        )

        @JvmStatic
        fun titleIdOf(handler: Class<out BaseCommand>): String =
            CAMEL_CASE_PAT.matcher(handler.simpleName).replaceAll("$1_$2").uppercase()
                .let { if (it.endsWith(PREFIX)) it.substring(0, it.length - PREFIX.length) else it }

        @JvmField
        val REFRESH = SubCommand.of(SystemCommands.REFRESH)

        @JvmField
        val GO_BACK = SubCommand.of(SystemCommands.GO_BACK)

        @JvmField
        val NOTHING = SubCommand.of(SystemCommands.NOTHING)
        private const val PREFIX = "_COMMAND"
        private val CAMEL_CASE_PAT = Pattern.compile("([a-z\\d])([A-Z]+)")
    }
}
