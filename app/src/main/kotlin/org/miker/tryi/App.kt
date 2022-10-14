package org.miker.tryi

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.miker.tryi.commands.Convert
import org.miker.tryi.commands.Evolve

class App : CliktCommand(name = "tryi") {
    override fun run() = Unit
}

fun main(args: Array<String>) {
    System.setProperty("apple.awt.UIElement", "true")
    App()
        .subcommands(Convert(), Evolve())
        .main(args)
}

// roulette change: change 1 of the 10 params
// gaussian mutate
