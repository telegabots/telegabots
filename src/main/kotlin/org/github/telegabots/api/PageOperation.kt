package org.github.telegabots.api

/**
 * Contains all possible operations with pages
 *
 * @see PageBuilder
 */
enum class PageOperation {
    /**
     * Creates new page into new block
     */
    Create,

    /**
     * Creates new page into existing (current) or specified block
     */
    Add,

    /**
     * Updates current page. If page/block not exists creates new
     */
    Update
}
