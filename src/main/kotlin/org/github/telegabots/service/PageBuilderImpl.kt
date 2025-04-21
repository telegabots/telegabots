package org.github.telegabots.service

import org.github.telegabots.MessageFile
import org.github.telegabots.api.*

/**
 * Implementation of [PageBuilder]
 */
internal class PageBuilderImpl(val message: String, val context: BaseContext) : PageBuilder {
    private var messageType: MessageType = MessageType.Text
    private var contentType: ContentType = ContentType.Plain
    private var disablePreview: Boolean = false
    private var subCommands: List<List<SubCommand>> = emptyList()
    private var handler: Class<out BaseCommand>? = null
    private var enableBack: Boolean = false
    private var id: Long = 0L
    private var blockId: Long = 0L
    private var state: StateRef? = null
    private var file: MessageFile? = null

    override fun create(): Long = context.createPage(createPage())

    override fun add(): Long? = context.addPage(createPage())

    override fun update(): Long? = context.updatePage(createPage())

    override fun id(id: Long?): PageBuilder {
        this.id = id ?: 0L
        return this
    }

    override fun blockId(blockId: Long?): PageBuilder {
        this.blockId = blockId ?: 0L
        return this
    }

    override fun state(state: StateRef?): PageBuilder {
        this.state = state
        return this
    }

    override fun file(file: MessageFile?): PageBuilder {
        this.file = file
        return this
    }

    override fun messageType(messageType: MessageType): PageBuilder {
        this.messageType = messageType
        return this
    }

    override fun contentType(contentType: ContentType): PageBuilder {
        this.contentType = contentType
        return this
    }

    override fun disablePreview(disable: Boolean): PageBuilder {
        this.disablePreview = disable
        return this
    }

    override fun enableBack(enable: Boolean): PageBuilder {
        this.enableBack = enable
        return this
    }

    override fun subCommands(subCommands: List<List<SubCommand>>): PageBuilder {
        this.subCommands = subCommands
        return this
    }

    override fun handler(handler: Class<out BaseCommand>?): PageBuilder {
        this.handler = handler
        return this
    }

    private fun createPage(): Page {
        return Page(
            message,
            contentType = contentType,
            messageType = messageType,
            disablePreview = disablePreview,
            subCommands = subCommands,
            handler = handler,
            id = id,
            blockId = blockId,
            state = state,
            file = file,
            enableBack = enableBack
        )
    }
}