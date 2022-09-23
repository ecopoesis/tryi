package org.miker.tryi

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics
import org.miker.tryi.ImageDiff.ImageDiffAlgo.LONG_DATABUFFER_LUT
import org.miker.tryi.ImageDiff.ImageDiffAlgo.LONG_DATABUFFER_LUT_PYTHAGOREAN
import java.awt.image.BufferedImage
import java.math.BigInteger.ZERO
import java.math.BigInteger.valueOf
import kotlin.math.abs

object ImageDiff {
    private val compositeLutLong: Array<LongArray> by lazy {
        (0..255).map { a ->
            (0..255).map { c ->
                composite(a.toUByte(), c.toUByte()).toLong()
            }.toTypedArray().toLongArray()
        }.toTypedArray()
    }

    private val compositeLutInt: Array<IntArray> by lazy {
        (0..255).map { a ->
            (0..255).map { c ->
                composite(a.toUByte(), c.toUByte())
            }.toTypedArray().toIntArray()
        }.toTypedArray()
    }

    private val squareLut: LongArray by lazy {
        (0..255).map { a ->
            (a * a).toLong()
        }.toTypedArray().toLongArray()
    }

    /**
     * the max distance for pythagorean diffs
     */
    private const val MAX_DIST: Long = (255 * 255) * 3L

    /**
     * adjust [color] by [alpha] for a black background
     */
    private fun composite(alpha: UByte, color: UByte): Int {
        // normalize 0..1
        val a = alpha.toLong() / 255.0
        val c = color.toLong() / 255.0

        return  ((a * c) * 255).toInt().coerceIn(0, 255)
    }

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
                if (a.type != BufferedImage.TYPE_4BYTE_ABGR && b.type != BufferedImage.TYPE_4BYTE_ABGR) {
                    throw IllegalArgumentException("Images must be TYPE_4BYTE_ABGR")
                }

                var total = 0L
                val aData = (a.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data
                val bData = (b.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data

                for (i in aData.indices) {
                    if (i % 4 == 0) {
                        // byte order is: A, B, G, R
                        total += abs(
                            composite(
                                aData[i].toUByte(),
                                aData[i + 1].toUByte()
                            ) - composite(bData[i].toUByte(), bData[i + 1].toUByte())
                        )
                        total += abs(
                            composite(
                                aData[i].toUByte(),
                                aData[i + 2].toUByte()
                            ) - composite(bData[i].toUByte(), bData[i + 2].toUByte())
                        )
                        total += abs(
                            composite(
                                aData[i].toUByte(),
                                aData[i + 3].toUByte()
                            ) - composite(bData[i].toUByte(), bData[i + 3].toUByte())
                        )
                    }
                }

                (total.toDouble() / (a.width * a.height * 3)) / 255
            }
        ),

        LONG_DATABUFFER_LUT(
            { a: BufferedImage, b: BufferedImage ->
                if (a.type != BufferedImage.TYPE_4BYTE_ABGR && b.type != BufferedImage.TYPE_4BYTE_ABGR) {
                    throw IllegalArgumentException("Images must be TYPE_4BYTE_ABGR")
                }
    
                var total = 0L
                val aData = (a.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data
                val bData = (b.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data
    
                for (i in aData.indices) {
                    if (i % 4 == 0) {
                        // byte order is: A, B, G, R
                        val alphaA = aData[i].toUByte().toInt()
                        val alphaB = bData[i].toUByte().toInt()
                        total += abs(
                            compositeLutLong[alphaA][aData[i + 1].toUByte().toInt()] -
                                    compositeLutLong[alphaB][bData[i + 1].toUByte().toInt()]
                        )
                        total += abs(
                            compositeLutLong[alphaA][aData[i + 2].toUByte().toInt()] -
                                    compositeLutLong[alphaB][bData[i + 2].toUByte().toInt()]
                        )
                        total += abs(
                            compositeLutLong[alphaA][aData[i + 3].toUByte().toInt()] -
                                    compositeLutLong[alphaB][bData[i + 3].toUByte().toInt()]
                        )
                    }
                }
    
                (total.toDouble() / (a.width * a.height * 3)) / 255
            }
        ),

        LONG_DATABUFFER_LUT_PYTHAGOREAN(
            { a: BufferedImage, b: BufferedImage ->
                if (a.type != BufferedImage.TYPE_4BYTE_ABGR && b.type != BufferedImage.TYPE_4BYTE_ABGR) {
                    throw IllegalArgumentException("Images must be TYPE_4BYTE_ABGR")
                }

                var total = 0L
                val aData = (a.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data
                val bData = (b.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data

                for (i in aData.indices) {
                    if (i % 4 == 0) {
                        // byte order is: A, B, G, R
                        val alphaA = aData[i].toUByte().toInt()
                        val alphaB = bData[i].toUByte().toInt()

                        val bDiff = compositeLutInt[alphaA][aData[i + 1].toUByte().toInt()] -
                                compositeLutInt[alphaB][bData[i + 1].toUByte().toInt()]

                        val gDiff = compositeLutInt[alphaA][aData[i + 2].toUByte().toInt()] -
                                compositeLutInt[alphaB][bData[i + 2].toUByte().toInt()]

                        val rDiff = compositeLutInt[alphaA][aData[i + 3].toUByte().toInt()] -
                                compositeLutInt[alphaB][bData[i + 3].toUByte().toInt()]

                        // no need for sqrt: it only makes things slower
                        total += (bDiff * bDiff) + (gDiff * gDiff) + (rDiff * rDiff)
                    }
                }

                (total.toDouble() / (a.width * a.height)) / MAX_DIST
            }
        ),

        LONG_DATABUFFER_LUT_PYTHAGOREAN_SQ_LUT(
            { a: BufferedImage, b: BufferedImage ->
                if (a.type != BufferedImage.TYPE_4BYTE_ABGR && b.type != BufferedImage.TYPE_4BYTE_ABGR) {
                    throw IllegalArgumentException("Images must be TYPE_4BYTE_ABGR")
                }

                var total = 0L
                val aData = (a.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data
                val bData = (b.alphaRaster.dataBuffer as java.awt.image.DataBufferByte).data

                for (i in aData.indices) {
                    if (i % 4 == 0) {
                        // byte order is: A, B, G, R
                        val alphaA = aData[i].toUByte().toInt()
                        val alphaB = bData[i].toUByte().toInt()

                        val bDiff = abs(compositeLutInt[alphaA][aData[i + 1].toUByte().toInt()] -
                                compositeLutInt[alphaB][bData[i + 1].toUByte().toInt()])

                        val gDiff = abs(compositeLutInt[alphaA][aData[i + 2].toUByte().toInt()] -
                                compositeLutInt[alphaB][bData[i + 2].toUByte().toInt()])

                        val rDiff = abs(compositeLutInt[alphaA][aData[i + 3].toUByte().toInt()] -
                                compositeLutInt[alphaB][bData[i + 3].toUByte().toInt()])

                        // no need for sqrt: it only makes things slower
                        total += squareLut[bDiff] + squareLut[gDiff] + squareLut[rDiff]
                    }
                }

                (total.toDouble() / (a.width * a.height)) / MAX_DIST
            }
        )

    }

    private fun Int.red(): Int = (this shr 16) and 0xff
    private fun Int.green(): Int = (this shr 8) and 0xff
    private fun Int.blue(): Int = this and 0xff

    /**
     * Compute the difference between two images. 0.0 is identical, 100.0 is opposites.
     */
    fun imageDiff(a: BufferedImage, b: BufferedImage, algo: ImageDiffAlgo = LONG_DATABUFFER_LUT_PYTHAGOREAN): Double {
        if (a.width != b.width || a.height != b.height) {
            throw IllegalArgumentException("Images must be the same size")
        }

        if (a.width.toLong() * a.height.toLong() > Long.MAX_VALUE / 3 / 255) {
            throw IllegalArgumentException("Images too big")
        }

        return algo.fn(a, b)
    }
}
