package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.GPIOManager

class StartServerCommand : Command(
    "startserver",
    "Startet den Server (und damit Terraria)",
    CommandCategory.Various,
    Constants.G.GENSHINEMPIRES
) {

    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val pc = GPIOManager.PC.FLORIX_2
        if (GPIOManager.isOn(pc)) {
            e.reply("Der Server ist bereits an!")
            return
        }
        GPIOManager.startServer(pc)
        e.reply("Der Server-PC wurde gestartet, der Terraria-Server dürfte demnächst online sein c:")
    }

}