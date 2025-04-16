package org.examples.testbot.commands

import org.github.telegabots.MessageFile
import org.github.telegabots.api.*
import org.github.telegabots.api.annotation.InlineHandler
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Sample command to show file via [MessageType.Photo]
 */
class FileViewCommand : BaseCommand() {

    @InlineHandler
    fun handleInline(message: String, currentStr: State<String>) {
        val file = File.createTempFile("temp", ".png")

        if (message == "clear") {
            currentStr.set("")
        } else if (SystemCommands.REFRESH != message) {
            currentStr.set((currentStr.get() ?: "") + message)
        }

        val pageOperation = if (context.messageType() == MessageType.Photo)
            PageOperation.Update
        else
            PageOperation.Create

        context.page(MessageFile.from(generatePng(currentStr.get() ?: "", file)))
            .subCommands(
                SubCommand.of("1"),
                SubCommand.of("2"),
                SubCommand.of("3"),
                SubCommand.of("clear"),
                SubCommand.of(FileViewCommand::class.java)
            )
            .messageType(MessageType.Photo)
            .enableBack()
            .apply(pageOperation)

        file.delete()
    }

    private fun generatePng(text: String, file: File): File {
        val width = 600
        val height = 400

        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2d = image.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.color = Color.WHITE
        g2d.fillRect(0, 0, width, height)

        if (text.isNotBlank()) {
            g2d.color = Color.BLACK
            val font = Font("Arial", Font.BOLD, 24)
            g2d.font = font
            val fm = g2d.fontMetrics
            val textWidth = fm.stringWidth(text)
            val textHeight = fm.ascent
            val x = (width - textWidth) / 2
            val y = (height + textHeight) / 2 - fm.descent
            g2d.drawString(text, x, y)
        }

        g2d.dispose()
        ImageIO.write(image, "png", file)
        return file
    }
}
