package org.github.telegabots.api

import org.github.telegabots.api.annotation.InlineHandler
import org.github.telegabots.api.annotation.TextHandler
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import java.util.regex.Pattern

/**
 * Data related with single button
 */
data class Button(
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
    val handler: Class<out BaseController>? = null,
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

    fun isSystemMessage() = this == REFRESH || this == GO_BACK

    /**
     * Creates new Button with new state
     */
    fun withState(state: StateRef?): Button =
        this.copy(state = state)

    companion object {
        inline fun <reified T : BaseController> of(
            state: StateRef? = null,
            titleId: String = "",
            title: String? = null
        ) = of(T::class.java, state, titleId, title)

        @JvmStatic
        fun of(
            titleId: String,
            title: String? = null,
            state: StateRef? = null
        ): Button = Button(
            titleId = titleId,
            title = title,
            state = state
        )

        @JvmStatic
        fun of(titleId: String): Button = of(titleId, null)

        @JvmStatic
        fun of(titleId: String, title: String): Button = of(titleId, title, null)

        @JvmStatic
        fun of(handler: Class<out BaseController>): Button = of(handler, null)

        @JvmStatic
        fun of(handler: Class<out BaseController>, state: StateRef): Button = of(handler, state, "")

        @JvmStatic
        fun of(handler: Class<out BaseController>, state: StateRef, titleId: String): Button =
            of(handler, state, titleId, null)

        @JvmStatic
        fun of(handler: Class<out BaseController>, titleId: String, title: String): Button =
            of(handler, null, titleId, title)

        @JvmStatic
        fun of(handler: Class<out BaseController>, titleId: String): Button =
            of(handler, null, titleId)

        @JvmStatic
        fun of(
            handler: Class<out BaseController>,
            state: StateRef? = null,
            titleId: String = "",
            title: String? = null
        ): Button = Button(
            titleId = if (titleId.isNotBlank()) titleId else titleIdOf(handler),
            handler = handler,
            state = state,
            title = title
        )

        @JvmStatic
        fun titleIdOf(handler: Class<out BaseController>): String =
            CAMEL_CASE_PAT.matcher(handler.simpleName).replaceAll("$1_$2").uppercase()
                .let { if (it.endsWith(PREFIX)) it.substring(0, it.length - PREFIX.length) else it }

        @JvmField
        val REFRESH = Button.of(SystemMessages.REFRESH)

        @JvmField
        val GO_BACK = Button.of(SystemMessages.GO_BACK)

        @JvmField
        val NOTHING = Button.of(SystemMessages.NOTHING)
        private const val PREFIX = "_CONTROLLER"
        private val CAMEL_CASE_PAT = Pattern.compile("([a-z\\d])([A-Z]+)")

        /**
         * Maximum length of titleId in bytes.
         */
        const val MAX_TITLE_ID_LENGTH = 64
    }
}
