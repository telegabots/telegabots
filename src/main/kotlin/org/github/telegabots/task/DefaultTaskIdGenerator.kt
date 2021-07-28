package org.github.telegabots.task

import org.github.telegabots.api.BaseTask
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Default generator for task.id() if user not override one
 */
object DefaultTaskIdGenerator {
    private val idCounts = ConcurrentHashMap<Class<out BaseTask>, Int>()
    private val cachedIds = WeakHashMap<BaseTask, String>()

    fun getId(task: BaseTask): String = cachedIds.computeIfAbsent(task) { generateId(task::class.java) }

    private fun generateId(clazz: Class<out BaseTask>): String = "${makeName(clazz)}-" + getNextClassCount(clazz)

    private fun getNextClassCount(clazz: Class<out BaseTask>): Int =
        idCounts.compute(clazz) { _, oldCount -> if (oldCount != null) oldCount + 1 else 1 }!!

    private fun makeName(clazz: Class<out BaseTask>): String =
        clazz.simpleName.let { if (it.endsWith("Task") && it.length > 4) it.substring(0, it.length - 4) else it }
}
