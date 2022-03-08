package org.miker.tryi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable.children
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
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
import javax.swing.text.html.HTML.Tag.P
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextUBytes

const val INTERNAL_RESOLUTION = 1000
const val NUM_TRIANGLES = 1000
const val CHILDREN = 10
const val IMAGE_DIFF_THRESHOLD = Long.MAX_VALUE / 3 / 255

class GeneratedPreview(val sizeX: Int, val sizeY: Int): JPanel() {

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

data class Point(val x: Int, val y: Int)
data class Color(val r: UByte, val g: UByte, val b: UByte, val a: UByte)
data class Triangle(val p1: Point, val p2: Point, val p3: Point, val color: org.miker.tryi.Color) {
    val x: IntArray by lazy { arrayOf(p1.x, p2.x, p3.x).toIntArray() }
    val y: IntArray by lazy { arrayOf(p1.y, p2.y, p3.y).toIntArray() }
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

    val ogImage = scaleImage(sourceImage, INTERNAL_RESOLUTION, INTERNAL_RESOLUTION)

    val triangles = initialize(NUM_TRIANGLES, ogImage, generatedPreview)
}

fun scaleImage(source: Image, x: Int, y: Int): BufferedImage {
    val scaledImage = source.getScaledInstance(x, y, Image.SCALE_SMOOTH)
    val bufferedImage = BufferedImage(x, y, BufferedImage.TYPE_4BYTE_ABGR)
    val g2d = bufferedImage.createGraphics()
    g2d.drawImage(scaledImage, 0, 0, null)
    g2d.dispose()
    return bufferedImage
}

fun BufferedImage.deepCopy(): BufferedImage = BufferedImage(this.colorModel, this.copyData(null), this.isAlphaPremultiplied, null)

/**
 * create the initial triangles. Randomly generates triangles until we have [numTriangles]. If the triangle makes the
 * image worse, we do not use it.
 */
@ExperimentalUnsignedTypes
fun initialize(numTriangles: Int, target: BufferedImage, preview: GeneratedPreview): List<Triangle> {
    fun addTriangle(triangles: List<Triangle>, image: BufferedImage): Triple<List<Triangle>, BufferedImage, Double> {
        val triangle = Random.nextUBytes(4).let { c ->
            Triangle(
                Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                Color(c[0], c[1], c[2], c[3])
            )
        }

        val candidate = image.deepCopy()
        val g2d = candidate.createGraphics()
        g2d.color = Color(
            triangle.color.r.toInt(),
            triangle.color.g.toInt(),
            triangle.color.b.toInt(),
            triangle.color.a.toInt()
        )
        g2d.fillPolygon(triangle.x, triangle.y, 3)
        g2d.dispose()

        return Triple(triangles + triangle, candidate, imageDiff(target, candidate))
    }

    tailrec fun inner(triangles: List<Triangle>, image: BufferedImage): Pair<List<Triangle>, BufferedImage> =
        when (triangles.size) {
            numTriangles -> Pair(triangles, image)
            else -> {
                val children = runBlocking {
                    (0 until CHILDREN).map {
                        async(Dispatchers.IO) {
                            addTriangle(triangles, image)
                        }
                    }.awaitAll()
                }
                val best = children.sortedBy { it.third }.last()
                println("Generated ${triangles.size}: ${best.third * 100}% correct")
                preview.update(best.second)

                inner(best.first, best.second)
            }
        }

    val (triangles, _) = inner(
        emptyList(),
        BufferedImage(INTERNAL_RESOLUTION, INTERNAL_RESOLUTION, BufferedImage.TYPE_4BYTE_ABGR)
    )
    return triangles
}

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
