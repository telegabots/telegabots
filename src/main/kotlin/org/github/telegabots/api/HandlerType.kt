package org.github.telegabots.api

/**
 * Type of handler method.
 */
enum class HandlerType {
    /**
     * Handler for text messages.
     */
    Text,

    /**
     * Handler for inline queries.
     */
    Inline,

    /**
     * Handler for any exception that occurs during processing of a request.
     *
     * Handlers of this type can be multiple in a controller.
     */
    Error
}
