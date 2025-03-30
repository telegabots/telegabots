package org.github.telegabots.api

import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.regex.Pattern

/**
 * Data related with single button
 */
data class SubCommand(
    /**
     * Data to be sent in a callback query to the bot when the button is pressed, 1-64 bytes. Refers to [InlineKeyboardButton.callbackData]
     */
    val titleId: String,
    /**
     * Label text on the button
     */
    val title: String? = null,
    /**
     * Handler class to be executed when the button is pressed
     *
     * Handler methods should be annotated with [TextHandler] or [InlineHandler]
     */
    val handler: Class<out BaseCommand>? = null,
    /**
     * Page and state behaviour
     */
    val behaviour: CommandBehaviour = CommandBehaviour.SeparatePage,
    /**
     * State to be used when handler is executed
     */
    val state: StateRef? = null
) {
    init {
        check(titleId.isNotEmpty()) { "TitleId is empty. Expected length is [1..$MAX_TITLE_ID_LENGTH] in bytes" }

        val titleIdSize = titleId.toByteArray().size
        check(titleIdSize <= MAX_TITLE_ID_LENGTH) {
            "TitleId is too long: $titleIdSize. Expected length is [1..$MAX_TITLE_ID_LENGTH] in bytes. TitleId: $titleId"
        }
    }

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

        /**
         * Maximum length of titleId in bytes.
         */
        const val MAX_TITLE_ID_LENGTH = 64
    }
}
