package org.miker.tryi

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import java.awt.Toolkit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextUBytes

const val IMAGE_DIFF_THRESHOLD = Long.MAX_VALUE / 3 / 255
const val UPDATE_RATE = 10L

const val FITNESS_THRESHOLD = 0.99

class GeneratedPreview(val sizeX: Int, val sizeY: Int): JPanel() {
    init {
        background = Color(0, 0, 0)
    }

    var image: Image = BufferedImage(sizeX, sizeY, BufferedImage.TYPE_4BYTE_ABGR)

    fun update(image: Image) {
        this.image = image.getScaledInstance(sizeX, sizeY, Image.SCALE_SMOOTH)
        repaint()
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        g.drawImage(image, 0, 0, null)
    }

    override fun getPreferredSize(): Dimension = Dimension(sizeX, sizeY)
}



data class Point(val x: UByte, val y: UByte) {
    fun mutate(amount: Double): Point {
        return Point(
            x = x.mutate(amount),
            y = y.mutate(amount)
        )
    }

    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun random(): Point {
            val bytes = Random.nextUBytes(2)
            return Point(bytes[0], bytes[1])
        }
    }
}

data class TryiColor(val r: UByte, val g: UByte, val b: UByte, val a: UByte) {
    fun mutate(amount: Double): TryiColor {
        return TryiColor(
            r = r.mutate(amount),
            g = g.mutate(amount),
            b = b.mutate(amount),
            a = a.mutate(amount),
        )
    }

    val asColor: Color by lazy { Color(r.toInt(), g.toInt(), b.toInt(), a.toInt()) }

    companion object {
        @OptIn(ExperimentalUnsignedTypes::class)
        fun random(): TryiColor {
            val bytes = Random.nextUBytes(4)
            return TryiColor(bytes[0], bytes[1], bytes[2], bytes[3])
        }
    }
}

data class Triangle(val p1: Point, val p2: Point, val p3: Point, val color: org.miker.tryi.TryiColor) {
    val x: IntArray by lazy { arrayOf(p1.x, p2.x, p3.x).map { it.toInt() }.toIntArray() }
    val y: IntArray by lazy { arrayOf(p1.y, p2.y, p3.y).map { it.toInt() }.toIntArray() }

    fun mutate(amount: Double): Triangle =
        Triangle(
            p1 = this.p1.mutate(amount),
            p2 = this.p2.mutate(amount),
            p3 = this.p3.mutate(amount),
            color = this.color.mutate(amount)
        )
}

/**
 * Triangles and their rendered image.
 */
data class Tryi(
    val triangles: List<Triangle>,
    val image: BufferedImage
)

/**
 * A tryi and how well it matches.
 */
data class TryiMatch(
    val tryi: Tryi,
    val diff: Double
) {
    constructor(triangles: List<Triangle>, image: BufferedImage, match: Double) : this(Tryi(triangles, image), match)

    fun image(): BufferedImage = tryi.image
    fun triangles(): List<Triangle> = tryi.triangles
}

@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    val screenSize = Toolkit.getDefaultToolkit().screenSize

    val source = JFrame("Source")
    val sourceImage = ImageIO.read(File("monalisa.jpg"))
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

    val ogImage = scaleImage(sourceImage)

    val evolver = SingleParentEvolver(ogImage, generatedPreview)
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

fun BufferedImage.deepCopy(): BufferedImage = BufferedImage(this.colorModel, this.copyData(null), this.isAlphaPremultiplied, null)


fun List<Triangle>.render(): BufferedImage {
    val out = BufferedImage(UByte.MAX_VALUE.toInt(), UByte.MAX_VALUE.toInt(), BufferedImage.TYPE_4BYTE_ABGR)
    val g2d = out.createGraphics()
    this.forEach { triangle ->
        g2d.color = triangle.color.asColor
        g2d.fillPolygon(triangle.x, triangle.y, 3)
    }
    g2d.dispose()
    return out
}

/**
 * Compute the difference between two images. 0.0 is identical, 100.0 is opposites.
 */
fun imageDiff(a: BufferedImage, b: BufferedImage): Double {
    fun slow(): Double {
        val stats = DescriptiveStatistics()

        for(x in 0 until a.width) {
            for(y in 0 until a.height) {
                val rgbA = a.getRGB(x, y)
                val rgbB = b.getRGB(x, y)

                stats.addValue(abs(((rgbA shr 16) and 0xff) - ((rgbB shr 16) and 0xff)).toDouble())
                stats.addValue(abs(((rgbA shr 8) and 0xff) - ((rgbB shr 8) and 0xff)).toDouble())
                stats.addValue(abs((rgbA and 0xff) - (rgbB and 0xff)).toDouble())
            }
        }

        return stats.mean / 255
    }

    fun fast(): Double {
        var total = 0L

        for (x in 0 until a.width) {
            for (y in 0 until a.height) {
                val rgbA = a.getRGB(x, y)
                val rgbB = b.getRGB(x, y)

                total += abs(((rgbA shr 16) and 0xff) - ((rgbB shr 16) and 0xff)).toLong()
                total += abs(((rgbA shr 8) and 0xff) - ((rgbB shr 8) and 0xff)).toLong()
                total += abs((rgbA and 0xff) - (rgbB and 0xff)).toLong()
            }
        }

        return (total.toDouble() / (a.width * a.height * 3)) / 255
    }

    if (a.width != b.width || a.height != b.height) {
        throw IllegalArgumentException("Images must be the same size")
    }

    return if (a.width.toLong() * a.height.toLong() >- IMAGE_DIFF_THRESHOLD) {
        slow()
    } else {
        fast()
    }
}
