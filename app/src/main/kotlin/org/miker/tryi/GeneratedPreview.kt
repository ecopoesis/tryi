package org.miker.tryi

import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Image
import java.awt.image.BufferedImage
import javax.swing.JPanel

class GeneratedPreview(val sizeX: Int, val sizeY: Int): JPanel() {
    init {
        background = Color(255, 255, 255)
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
