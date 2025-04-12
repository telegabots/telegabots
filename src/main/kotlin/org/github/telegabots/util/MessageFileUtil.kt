package org.github.telegabots.util

import org.github.telegabots.MessageFile
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto

fun MessageFile.toInputFile(): InputFile {
    return if (isFile()) {
        InputFile(getFile(), getName())
    } else if (isFileId()) {
        InputFile(getFileId())
    } else {
        error("It's not a file or fileId but: $this")
    }
}

fun MessageFile.toInputMediaPhoto(): InputMediaPhoto {
    val photo = InputMediaPhoto()

    if (isFile()) {
        photo.setMedia(getFile(), getName())
    } else if (isFileId()) {
        photo.media = getFileId()
    } else {
        error("It's not a file or fileId but: $this")
    }

    return photo
}