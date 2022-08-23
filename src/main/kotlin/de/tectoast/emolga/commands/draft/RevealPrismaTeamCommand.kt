package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.buttons.buttonsaves.PrismaTeam
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.Emolga
import net.dv8tion.jda.api.interactions.components.buttons.Button

class RevealPrismaTeamCommand :
    Command("revealprismateam", "Revealt die Prisma Teams lol", CommandCategory.Draft, Constants.G.FLP) {
    init {
        setCustomPermissions(PermissionPreset.fromIDs(297010892678234114L))
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user", "User", "Der User lol", ArgumentManagerTemplate.DiscordType.USER)
            .setExample("!revealprismateam @HennyHahn")
            .build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val user = e.arguments.getMember("user")
        val prisma = Emolga.get.league("Prisma")
        val picks = prisma.picks
        val jsonList = picks[user.idLong]!!.reversed()
        e.textChannel.sendMessage("**" + user.effectiveName + "**")
            .setActionRow(Button.primary("prisma;lol", "Nächstes Pokemon"))
            .queue { message ->
                prismaTeam[message.idLong] = PrismaTeam(
                    jsonList.map { it.name }, prisma.table.indexOf(user.idLong)
                )
            }
    }
}