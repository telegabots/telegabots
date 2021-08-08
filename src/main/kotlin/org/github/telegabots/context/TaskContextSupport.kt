package org.github.telegabots.context

import org.github.telegabots.api.TaskContext

/**
 * Supports TaskContext for current executing task
 */
object TaskContextSupport : BaseContextSupport<TaskContext>(), TaskContext {
}

