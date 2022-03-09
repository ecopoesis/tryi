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
import java.time.LocalDateTime
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.text.html.HTML.Tag.P
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextUBytes

const val INTERNAL_RESOLUTION = 200
const val NUM_TRIANGLES = 150
const val CHILDREN = 50
const val IMAGE_DIFF_THRESHOLD = Long.MAX_VALUE / 3 / 255
const val UPDATE_RATE = 10L

/**
 * Chance any triangle will mutate.
 */
const val MUTATION_CHANCE_PCT = 1

/**
 * Amount a triangle will mutate.
 */
const val MUTATION_AMOUNT_PCT = 10 / 200.0

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

data class Point(val x: Int, val y: Int) {
    fun mutate(amount: Double): Point {
        val xChange = max((x * amount).roundToInt(), 1)
        val yChange = max((y * amount).roundToInt(), 1)
        return Point(
            x = Random.nextInt(x - xChange, x + xChange).coerceIn(0, INTERNAL_RESOLUTION),
            y = Random.nextInt(y - yChange, y + yChange).coerceIn(0, INTERNAL_RESOLUTION)
        )
    }
}

data class Color(val r: Int, val g: Int, val b: Int, val a: Int) {
    fun mutate(amount: Double): org.miker.tryi.Color {
        val rChange = max((r * amount).roundToInt(), 1)
        val gChange = max((g * amount).roundToInt(), 1)
        val bChange = max((b * amount).roundToInt(), 1)
        val aChange = max((a * amount).roundToInt(), 1)

        return Color(
            r = Random.nextInt(r - rChange, r + rChange).coerceIn(0, 255),
            g = Random.nextInt(g - gChange, g + gChange).coerceIn(0, 255),
            b = Random.nextInt(b - bChange, b + bChange).coerceIn(0, 255),
            a = Random.nextInt(a - aChange, a + aChange).coerceIn(0, 255)
        )
    }
}

data class Triangle(val p1: Point, val p2: Point, val p3: Point, val color: org.miker.tryi.Color) {
    val x: IntArray by lazy { arrayOf(p1.x, p2.x, p3.x).toIntArray() }
    val y: IntArray by lazy { arrayOf(p1.y, p2.y, p3.y).toIntArray() }

    fun mutate(amount: Double): Triangle =
        Triangle(
            p1 = this.p1.mutate(amount),
            p2 = this.p2.mutate(amount),
            p3 = this.p3.mutate(amount),
            color = this.color.mutate(amount)
        )
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

    val (triangles, base) = initialize(NUM_TRIANGLES, ogImage, generatedPreview)

    val result = evolve(triangles, base, ogImage, generatedPreview)
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
 * Create the initial triangles. Randomly generates triangles until we have [numTriangles].
 * For each new triangle, [CHILDREN] are generated and the best is chosen.
 */
@ExperimentalUnsignedTypes
fun initialize(numTriangles: Int, target: BufferedImage, preview: GeneratedPreview): Pair<List<Triangle>, BufferedImage> {
    fun addTriangle(triangles: List<Triangle>, image: BufferedImage): Triple<List<Triangle>, BufferedImage, Double> {
        val triangle =
            Triangle(
                Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                org.miker.tryi.Color(
                    Random.nextInt(0, 255),
                    Random.nextInt(0, 255),
                    Random.nextInt(0, 255),
                    Random.nextInt(0, 255)
                )
            )

        val candidate = image.deepCopy()
        val g2d = candidate.createGraphics()
        g2d.color = Color(
            triangle.color.r,
            triangle.color.g,
            triangle.color.b,
            triangle.color.a
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
                val best = children.sortedBy { it.third }.first()
                println("Generated ${triangles.size}: ${(1 - best.third) * 100}% correct")
                preview.update(best.second)

                inner(best.first, best.second)
            }
        }

    val (triangles, image) = inner(
        emptyList(),
        BufferedImage(INTERNAL_RESOLUTION, INTERNAL_RESOLUTION, BufferedImage.TYPE_4BYTE_ABGR)
    )
    return Pair(triangles, image)
}

fun evolve(triangles: List<Triangle>, base: BufferedImage, target: BufferedImage, preview: GeneratedPreview): List<Triangle> {
    fun mutate(parent: List<Triangle>): Triple<List<Triangle>, BufferedImage, Double> {
        val child = parent.map { triangle ->
            when (Random.nextInt(1, 101) <= MUTATION_CHANCE_PCT) {
                false -> triangle
                else -> triangle.mutate(MUTATION_AMOUNT_PCT)
            }
        }
        val candidate = BufferedImage(INTERNAL_RESOLUTION, INTERNAL_RESOLUTION, BufferedImage.TYPE_4BYTE_ABGR)
        val g2d = candidate.createGraphics()
        child.forEach { triangle ->
            g2d.color = Color(
                triangle.color.r,
                triangle.color.g,
                triangle.color.b,
                triangle.color.a
            )
            g2d.fillPolygon(triangle.x, triangle.y, 3)
        }
        g2d.dispose()

        return Triple(child, candidate, imageDiff(target, candidate))
    }

    tailrec fun inner(triangles: List<Triangle>, image: BufferedImage, diff: Double, generation: Long, start: Long): Triple<List<Triangle>, BufferedImage, Double> =
        when {
            diff >= FITNESS_THRESHOLD -> Triple(triangles, image, diff)
            else -> {
                val children = runBlocking {
                    (0 until CHILDREN).map {
                        async(Dispatchers.IO) {
                            mutate(triangles)
                        }
                    }.awaitAll()
                }
                val best = children.sortedBy { it.third }.first()
                if (best.third < diff) {
                    val msg = "Evolved ($generation): ${(1 - best.third) * 100}% correct"
                    if (generation % 1 == UPDATE_RATE) {
                        preview.update(best.second)
                        println("$msg - ${(generation.toDouble() / (System.currentTimeMillis() - start)) * 1000} generations/sec")
                    } else {
                        println(msg)
                    }
                    inner(best.first, best.second, best.third, generation + 1, start)
                } else {
                    println("Bad ($generation): ${(1 - diff) * 100}% correct")
                    inner(triangles, image, diff, generation + 1, start)
                }
            }
        }

    val (evolved, _, _) = inner(
        triangles,
        base,
        imageDiff(target, base),
        0,
        System.currentTimeMillis()
    )
    return evolved
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
