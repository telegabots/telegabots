package org.github.telegabots

import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

/**
 * Represents a file or file id that can be sent.
 *
 * File id can be http link to file.
 */
data class MessageFile(
    private val file: File?,
    private val name: String?,
    private val fileId: String?
) {
    @JsonIgnore
    fun getFile(): File = file ?: error("It's not a file")

    @JsonIgnore
    fun getName(): String = name ?: getFile().name

    @JsonIgnore
    fun getFileId(): String = fileId ?: error("It's not a fileId")

    @JsonIgnore
    fun isFile(): Boolean = file != null

    @JsonIgnore
    fun isFileId(): Boolean = fileId != null

    companion object {
        @JvmStatic
        fun from(file: File): MessageFile = MessageFile(file, file.name, null)

        @JvmStatic
        fun from(file: File, name: String): MessageFile = MessageFile(file, name, null)

        @JvmStatic
        fun from(fileId: String): MessageFile = MessageFile(null, null, fileId)
    }
}
