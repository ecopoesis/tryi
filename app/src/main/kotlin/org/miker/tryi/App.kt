package org.miker.tryi

import arrow.core.None
import arrow.core.toOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.double
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.miker.tryi.Evolver.Companion.NUM_TRIANGLES
import org.miker.tryi.Triangle.MutationType
import org.miker.tryi.Triangle.MutationType.FULL
import java.awt.Image
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import kotlin.math.roundToInt

class App : CliktCommand() {
    private val outputRate: Int by option("-r", "--rate", help="How often to output a tryi file.").int()
        .default(50)
    private val preview: Boolean by option("-p", "--preview", help="Show preview window.").flag(default = false)
    private val input: File by argument(help="Input file.")
        .file(mustExist = true, mustBeReadable = true, canBeDir = false)
    private val algo: String by option("-a", "--algo", help="Algorithm to use.")
        .choice("single", "multi").required()
    private val triangles: Int by option("-t", "--triangles", help="How many triangles in the tryi.")
        .int().default(NUM_TRIANGLES)
    private val mutationType: MutationType by option("-m", "--mutation", help="What style of mutation to use.")
        .enum<MutationType>().default(FULL)
    private val mutationChance: Double by option("--chance", help="Chance of a mutation occurring (0..1).")
        .double().default(0.01)
    private val mutationAmount: Double by option("--amount", help="Mutation amount (0..1).")
        .double().default(0.10)

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

        val evolver: Evolver = when (algo) {
            "single" -> SingleParentEvolver(
                ogImage,
                previewFrame,
                input.nameWithoutExtension,
                ogX = sourceImage.width,
                ogY = sourceImage.height,
                numChildren = cores,
                numTriangles = triangles,
                mutationAmount = mutationAmount,
                mutationChance = mutationChance,
                mutationType = mutationType
            )
            "multi" -> MultiParentEvolver(
                ogImage,
                previewFrame,
                input.nameWithoutExtension,
                ogX = sourceImage.width,
                ogY = sourceImage.height,
                numTriangles = triangles,
                mutationAmount = mutationAmount,
                mutationChance = mutationChance,
                mutationType = mutationType
            )
            else -> throw IllegalArgumentException("unknown algo ${algo}")
        }

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
