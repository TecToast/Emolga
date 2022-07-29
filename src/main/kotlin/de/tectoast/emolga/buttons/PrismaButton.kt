package de.tectoast.emolga.buttons

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.x
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.RequestBuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PrismaButton : ButtonListener("prisma") {
    override fun process(e: ButtonInteractionEvent, name: String) {
        val id = e.user.idLong
        if (id != 297010892678234114L && id != 213725720407441410L && id != Constants.FLOID) {
            e.reply("nö c:").setEphemeral(true).queue()
            return
        }
        val pt = Command.prismaTeam[e.messageIdLong]
        if (pt == null) {
            e.reply(":(").queue()
            return
        }
        val pokemonData = pt.nextMon()
        RequestBuilder.updateSingle(
            "1nCPIc-R5hAsoDXvTGSuGyk2c1K8DQqTBm1NGvLyYYm0",
            "Teamübersicht!${pt.index.x(3, 2)}${pokemonData.ycoord}",
            pokemonData.pokemon
        )
        e.reply("+1").setEphemeral(true).queue()
    }
}