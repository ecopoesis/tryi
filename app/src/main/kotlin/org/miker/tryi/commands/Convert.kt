package org.miker.tryi.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import org.miker.tryi.Tryi
import org.miker.tryi.commands.Format.DNA
import org.miker.tryi.commands.Format.TRYI

enum class Format {
    TRYI,

    /**
     * alteredqualia DNA
     */
    DNA
}

class Convert : CliktCommand(help = "Convert formats.") {
    private val format: Format by option("-f", "--format", help="Input format.").enum<Format>().required()
    private val input: String by option("-i", "--input", help="Value to convert.").required()

    override fun run() {
        val tryi = when (format) {
            TRYI -> Tryi.deserialize(input)
            DNA -> Tryi.readDna(input)
        }

        println(tryi.serialize())
    }
}
