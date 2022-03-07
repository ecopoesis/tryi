/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package org.miker.tryi

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import java.awt.Toolkit
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.nextUBytes

const val INTERNAL_RESOLUTION = 1000

class GeneratedPreview(val sizeX: Int, val sizeY: Int): JPanel() {

    var image: Image = BufferedImage(sizeX, sizeY, BufferedImage.TYPE_4BYTE_ABGR)

    fun update(image: Image) {
        this.image = image
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
    val triangles = mutableListOf<Triangle>()

    val source = JFrame("Source")
    val icon = ImageIcon("monalisa.jpg")
    val scaledHeight = (screenSize.height * 0.7).toInt()
    val scaledWidth = (icon.iconWidth * (scaledHeight.toDouble() / icon.iconHeight)).roundToInt()
    val scaled = ImageIcon(icon.image.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH))
    val label = JLabel(scaled)
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

    val xScale = INTERNAL_RESOLUTION / scaledWidth.toDouble()
    val yScale = INTERNAL_RESOLUTION / scaledHeight.toDouble()

    while (true) {
        triangles.add(
            Random.nextUBytes(4).let { c ->
                Triangle(
                    Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                    Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                    Point(Random.nextInt(0, INTERNAL_RESOLUTION), Random.nextInt(0, INTERNAL_RESOLUTION)),
                    Color(c[0], c[1], c[2], c[3])
                )
            }
        )

        val image = BufferedImage(scaledHeight, scaledHeight, BufferedImage.TYPE_4BYTE_ABGR)
        val g2d = image.createGraphics()

        triangles.forEach { triangle ->
            g2d.color = Color(
                triangle.color.r.toInt(),
                triangle.color.g.toInt(),
                triangle.color.b.toInt(),
                triangle.color.a.toInt()
            )
            g2d.fillPolygon(
                triangle.x.map { (it / xScale).roundToInt() }.toIntArray(),
                triangle.y.map { (it / yScale).roundToInt() }.toIntArray(),
                3
            )
        }

        generatedPreview.update(image)
    }
}
