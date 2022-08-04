package de.tectoast.emolga.commands.pokemon

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import java.util.*

class RandomizeKillsCommand : Command("randomizekills", "Randomized die Kills auf 6 Mons", CommandCategory.Pokemon) {
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val l: MutableList<Int> = LinkedList()
        val r = Random()
        for (i in 0..5) {
            var rand = r.nextInt(6) + 1
            val sum = l.sum()
            if (sum + rand > 6) rand = 6 - sum
            l.add(rand)
        }
        l.shuffle()
        e.reply(l.indices.joinToString("\n") { "Pokemon ${it + 1}: ${l[it]}" })
    }
}