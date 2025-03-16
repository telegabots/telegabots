package org.github.telegabots.api

/**
 * Builder for [Page].
 *
 * see [BaseContext]
 */
interface PageBuilder {
    /**
     * Creates new page into new block
     *
     * Returns created page id
     */
    fun create(): Long

    /**
     * Creates new page into existing (current) or specified block
     *
     * Returns created page id or null if specified block not found
     */
    fun add(): Long?

    /**
     * Updates current page. If page/block not exists creates new
     *
     * Returns updated/created page id or null if page already removed
     */
    fun update(): Long?

    /**
     * Set id for [Page]
     */
    fun id(id: Long): PageBuilder

    /**
     * Set block id for [Page]
     */
    fun blockId(blockId: Long): PageBuilder

    /**
     * Set [State] for [Page]
     */
    fun state(state: StateRef): PageBuilder

    /**
     * Set message type for page. Default is [MessageType.Text]
     */
    fun messageType(messageType: MessageType): PageBuilder

    /**
     * Set content type for page. Default is [ContentType.Plain]
     */
    fun contentType(contentType: ContentType): PageBuilder

    /**
     * Disable preview for links in message. Default is false
     */
    fun disablePreview(): PageBuilder {
        return disablePreview(true)
    }

    /**
     * Disable preview for links in message. Default is false
     */
    fun disablePreview(disable: Boolean): PageBuilder

    /**
     * Enable back button for [Page]. Default is false
     */
    fun enableBack(): PageBuilder {
        return enableBack(true)
    }

    /**
     * Enable back button for [Page]. Default is false
     */
    fun enableBack(enable: Boolean): PageBuilder

    /**
     * Set sub-commands for [Page].
     */
    fun subCommands(subCommands: List<List<SubCommand>>): PageBuilder

    /**
     * Set sub-commands for [Page].
     */
    fun subCommands(vararg subCommands: SubCommand): PageBuilder {
        // convert list to vertical layout
        return subCommands(subCommands.map { cmd -> listOf(cmd) })
    }

    /**
     * Set sub-commands by [BaseCommand].
     */
    fun subCommands(vararg commands: Class<out BaseCommand>): PageBuilder {
        // convert list to vertical layout
        return subCommands(commands.map { cmd -> listOf(SubCommand.of(cmd)) })
    }

    /**
     * Set handler for [Page]
     */
    fun handler(handler: Class<out BaseCommand>): PageBuilder
}