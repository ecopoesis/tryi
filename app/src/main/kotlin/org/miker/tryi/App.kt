package org.miker.tryi

import arrow.core.None
import arrow.core.toOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import java.awt.Image
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.math.roundToInt

const val OUTPUT_RATE = 50L

class App : CliktCommand() {
    val outputRate: Int by option("-r", "--rate", help="How often to output a tryi file.").int().default(50)
    val preview: Boolean by option("-p", "--preview", help="Show preview window.").flag(default = false)
    val input: File by argument(help="Input file.").file(mustExist = true, mustBeReadable = true, canBeDir = false)

    override fun run() {
        val cores = Runtime.getRuntime().availableProcessors()
        println("number of cores: $cores")

        val sourceImage = ImageIO.read(input)

        val previewFrame = when {
            preview -> {
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                val source = JFrame("Source")
                val scaledHeight = (screenSize.height * 0.7).toInt()
                val scaledWidth = (sourceImage.width * (scaledHeight.toDouble() / sourceImage.height)).roundToInt()
                val scaledImage = sourceImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH)
                val label = JLabel(ImageIcon(scaledImage))
                source.add(label)
                source.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                source.pack()
                source.setLocation(0, 0)
                source.isVisible = true

                val generatedPreview = GeneratedPreview(scaledWidth, scaledHeight)
                val generated = JFrame("Generated")
                generated.contentPane.add(generatedPreview)
                generated.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                generated.pack()
                generated.setLocation(scaledWidth, 0)
                generated.isVisible = true

                generatedPreview.toOption()
            }
            else -> None
        }

        val ogImage = scaleImage(sourceImage)

        val evolver: Evolver = MultiParentEvolver(ogImage, previewFrame, outputRate, input.nameWithoutExtension)
        evolver.evolve()
    }

    fun scaleImage(source: Image): BufferedImage {
        val scaledImage = source.getScaledInstance(UByte.MAX_VALUE.toInt(), UByte.MAX_VALUE.toInt(), Image.SCALE_SMOOTH)
        val bufferedImage = BufferedImage(UByte.MAX_VALUE.toInt(), UByte.MAX_VALUE.toInt(), BufferedImage.TYPE_4BYTE_ABGR)
        val g2d = bufferedImage.createGraphics()
        g2d.drawImage(scaledImage, 0, 0, null)
        g2d.dispose()
        return bufferedImage
    }
}

fun main(args: Array<String>) = App().main(args)
