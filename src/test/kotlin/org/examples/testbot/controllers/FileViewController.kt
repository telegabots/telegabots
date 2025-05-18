package org.examples.testbot.controllers

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
 * Sample controller to show file via [MessageType.Photo]
 */
class FileViewController : BaseController() {

    @InlineHandler
    fun handleInline(message: String, currentStr: State<String>) {
        val file = File.createTempFile("temp", ".png")

        if (message == "clear") {
            currentStr.set("")
        } else if (SystemMessages.REFRESH != message) {
            currentStr.set((currentStr.get() ?: "") + message)
        }

        context.page(MessageFile.from(generatePng(currentStr.get() ?: "", file)))
            .buttons(
                Button.of("1"),
                Button.of("2"),
                Button.of("3"),
                Button.of("clear"),
                Button.of(FileViewController::class.java)
            )
            .messageType(MessageType.Photo)
            .enableBack()
            .update()

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
