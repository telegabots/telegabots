package org.examples.allfeaturedbot.tasks

import org.github.telegabots.api.BaseTask
import org.github.telegabots.api.StateRef
import org.github.telegabots.api.annotation.TaskHandler
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Task calculating full size of defined dir path
 */
class CalculateDirSizeTask(private val dirPath: String) : BaseTask() {
    private val log = LoggerFactory.getLogger(javaClass)
    private val progress = AtomicInteger()
    private val status = AtomicReference("")
    private val stopping = AtomicBoolean()

    override fun title(): String = "Calculates dir full size"

    override fun stopAsync() = stopping.set(true)

    override fun status(): String = status.get()

    override fun progress(): Int = progress.get()

    @TaskHandler
    override fun run() {
        if (!stopping.get()) {
            progress.set(0)
            val fileCount = AtomicInteger()
            val root = File(dirPath)
            val dirSize = calculateDirSize(root, fileCount) { file: File, index: Int, size: Int ->
                val percent = ((index + 1).toDouble() * 100 / size).toInt()
                status.set("Scanned " + file.absolutePath)
                val oldPercent = progress.getAndSet(percent)

                if (oldPercent != percent) {
                    context.refreshPage(state = StateRef.of(CalculateDirSizeProgressInfo(status.get(), percent)), pageId = context.pageId())
                }
            }
            status.set("Scanned ${fileCount.get()} files")
            progress.set(100)

            log.info("Calculated size: $dirSize bytes after scanned ${fileCount.get()} files in ${root.absolutePath}")
        }

        stopping.set(false)
    }

    private fun calculateDirSize(
        file: File,
        fileCount: AtomicInteger,
        handler: ((File, Int, Int) -> Unit)? = null
    ): Long {
        if (file.exists()) {
            if (file.isDirectory) {
                log.debug("Scanning dir $file...")
                val files = file.listFiles()?.toList() ?: emptyList<File>()
                var curDirSize: Long = 0

                files.forEachIndexed { index, fileChild ->
                    curDirSize += calculateDirSize(fileChild, fileCount)

                    if (stopping.get()) {
                        return 0L
                    }

                    if (handler != null) {
                        handler(fileChild, index, files.size)
                    }
                }

                return curDirSize
            } else if (file.isFile) {
                fileCount.incrementAndGet()
                return file.length()
            }
        }

        return 0L
    }
}

data class CalculateDirSizeProgressInfo(val status: String, val percent: Int)
