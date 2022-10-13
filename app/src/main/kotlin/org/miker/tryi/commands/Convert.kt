package org.miker.tryi.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import org.miker.tryi.Tryi
import org.miker.tryi.commands.Format.DNA
import org.miker.tryi.commands.Format.JPEG
import org.miker.tryi.commands.Format.PNG
import org.miker.tryi.commands.Format.TRYI
import java.awt.Color
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

enum class Format {
    TRYI,

    /**
     * alteredqualia DNA
     */
    DNA,

    JPEG,
    PNG
}

class Convert : CliktCommand(help = "Convert formats.") {
    private val input: File by argument(help="Input file.")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
    private val format: Format by option("-f", "--format", help="Output format.").enum<Format>().required()

    override fun run() {
        val tryi = when (input.extension) {
            "tryi" -> Tryi.deserialize(input.readText())
            "dna" -> Tryi.readDna(input.readText())
            else -> throw IllegalArgumentException("Unsupported input ${input.extension}")
        }

        when (format) {
            TRYI -> File("${input.nameWithoutExtension}.tryi").writeText(tryi.serialize())
            JPEG, PNG -> {
                val toolkitImage = tryi.image.getScaledInstance(tryi.x, tryi.y, Image.SCALE_SMOOTH);
                val output = BufferedImage(tryi.x, tryi.y, BufferedImage.TYPE_INT_ARGB)
                val g = output.getGraphics()
                g.drawImage(toolkitImage, 0, 0, Color(255, 255, 255),null)
                g.dispose()
                ImageIO.write(
                    output,
                    format.toString(),
                    File("${input.nameWithoutExtension}.${format.toString().lowercase()}")
                )
            }
            else -> throw IllegalArgumentException("Unsupported format ${format}")
        }
    }
}
