package org.miker.tryi

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.miker.tryi.ImageDiff.ImageDiffAlgo.LONG_DATABUFFER
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_4BYTE_ABGR
import java.awt.image.DataBufferByte
import java.math.BigInteger.*
import kotlin.math.abs

object ImageDiff {
    enum class ImageDiffAlgo(val fn: (a: BufferedImage, b: BufferedImage) -> Double) {
        APACHE_STATS(
            { a: BufferedImage, b: BufferedImage ->
                val stats = DescriptiveStatistics(a.width * a.height * 3)

                for (x in 0 until a.width) {
                    for (y in 0 until a.height) {
                        val rgbA = a.getRGB(x, y)
                        val rgbB = b.getRGB(x, y)

                        stats.addValue(abs(rgbA.red() - rgbB.red()).toDouble())
                        stats.addValue(abs(rgbA.green() - rgbB.green()).toDouble())
                        stats.addValue(abs(rgbA.blue() - rgbB.blue()).toDouble())
                    }
                }

                stats.mean / 255
            }
        ),

        BIG_INT(
            { a: BufferedImage, b: BufferedImage ->
                var total = ZERO

                for (x in 0 until a.width) {
                    for (y in 0 until a.height) {
                        val rgbA = a.getRGB(x, y)
                        val rgbB = b.getRGB(x, y)

                        total = total.add(valueOf(abs(rgbA.red() - rgbB.red()).toLong()))
                        total = total.add(valueOf(abs(rgbA.green() - rgbB.green()).toLong()))
                        total = total.add(valueOf(abs(rgbA.blue() - rgbB.blue()).toLong()))
                    }
                }

                (total.toDouble() / (a.width * a.height * 3)) / 255
            }
        ),

        LONG(
            { a: BufferedImage, b: BufferedImage ->
                var total = 0L

                for (x in 0 until a.width) {
                    for (y in 0 until a.height) {
                        val rgbA = a.getRGB(x, y)
                        val rgbB = b.getRGB(x, y)

                        total += abs(rgbA.red() - rgbB.red()).toLong()
                        total += abs(rgbA.green() - rgbB.green()).toLong()
                        total += abs(rgbA.blue() - rgbB.blue()).toLong()
                    }
                }

                (total.toDouble() / (a.width * a.height * 3)) / 255
            }
        ),

        LONG_ARRAY(
            { a: BufferedImage, b: BufferedImage ->
                var total = 0L

                val aArr = a.getRGB(0, 0, a.width, a.height, null, 0, a.width)
                val bArr = b.getRGB(0, 0, b.width, b.height, null, 0, b.width)

                for (i in aArr.indices) {
                    val rgbA = aArr[i]
                    val rgbB = bArr[i]

                    total += abs(rgbA.red() - rgbB.red()).toLong()
                    total += abs(rgbA.green() - rgbB.green()).toLong()
                    total += abs(rgbA.blue() - rgbB.blue()).toLong()
                }

                (total.toDouble() / (a.width * a.height * 3)) / 255
            }
        ),

        LONG_DATABUFFER(
            { a: BufferedImage, b: BufferedImage ->
                /**
                 * adjust [color] by [alpha] for a black background
                 */
                fun composite(alpha: kotlin.UByte, color: kotlin.UByte): kotlin.Long {
                    // normalize 0..1
                    val a = alpha.toLong() / 255.0
                    val c = color.toLong() / 255.0

                    return  ((a * c) * 255).toLong().coerceIn(0, 255)
                }

                if (a.type != java.awt.image.BufferedImage.TYPE_4BYTE_ABGR && b.type != java.awt.image.BufferedImage.TYPE_4BYTE_ABGR) {
                    throw IllegalArgumentException("Images must be TYPE_4BYTE_ABGR")
                }

                var total = 0L
                val aData = (a.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data
                val bData = (b.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data

                for (i in aData.indices) {
                    if (i % 4 == 0) {
                        // byte order is: A, B, G, R
                        total += kotlin.math.abs(
                            composite(
                                aData[i].toUByte(),
                                aData[i + 1].toUByte()
                            ) - composite(bData[i].toUByte(), bData[i + 1].toUByte())
                        )
                        total += kotlin.math.abs(
                            composite(
                                aData[i].toUByte(),
                                aData[i + 2].toUByte()
                            ) - composite(bData[i].toUByte(), bData[i + 2].toUByte())
                        )
                        total += kotlin.math.abs(
                            composite(
                                aData[i].toUByte(),
                                aData[i + 3].toUByte()
                            ) - composite(bData[i].toUByte(), bData[i + 3].toUByte())
                        )
                    }
                }

                (total.toDouble() / (a.width * a.height * 3)) / 255
            }
        )
    }

    private fun Int.red(): Int = (this shr 16) and 0xff
    private fun Int.green(): Int = (this shr 8) and 0xff
    private fun Int.blue(): Int = this and 0xff

    /**
     * Compute the difference between two images. 0.0 is identical, 100.0 is opposites.
     */
    fun imageDiff(a: BufferedImage, b: BufferedImage, algo: ImageDiffAlgo = LONG_DATABUFFER): Double {
        if (a.width != b.width || a.height != b.height) {
            throw IllegalArgumentException("Images must be the same size")
        }

        if (a.width.toLong() * a.height.toLong() > Long.MAX_VALUE / 3 / 255) {
            throw IllegalArgumentException("Images too big")
        }

        return algo.fn(a, b)
    }
}
