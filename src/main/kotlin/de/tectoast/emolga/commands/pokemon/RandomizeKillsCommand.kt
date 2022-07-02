package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import java.util.*

class RandomizeKillsCommand : Command("randomizekills", "Randomized die Kills auf 6 Mons", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override fun process(e: GuildCommandEvent) {
        val l: MutableList<Int?> = LinkedList()
        val r = Random()
        for (i in 0..5) {
            var rand = r.nextInt(6) + 1
            val sum = l.stream().mapToInt { x: Int? -> x!! }.sum()
            if (sum + rand > 6) rand = 6 - sum
            l.add(rand)
        }
        l.shuffle()
        val b = StringBuilder()
        for (i in l.indices) {
            b.append("Pokemon ").append(i + 1).append(": ").append(l[i]).append("\n")
        }
        e.reply(b.toString())
    }
}