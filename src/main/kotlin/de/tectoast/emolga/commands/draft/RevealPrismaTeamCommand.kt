package de.tectoast.emolga.commands.draft

import de.tectoast.emolga.buttons.buttonsaves.PrismaTeam
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Constants
import de.tectoast.jsolf.JSONObject
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.buttons.Button

class RevealPrismaTeamCommand :
    Command("revealprismateam", "Revealt die Prisma Teams lol", CommandCategory.Draft, Constants.FLPID) {
    init {
        setCustomPermissions(PermissionPreset.fromIDs(297010892678234114L))
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user", "User", "Der User lol", ArgumentManagerTemplate.DiscordType.USER)
            .setExample("!revealprismateam @HennyHahn")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val user = e.arguments!!.getMember("user")
        val prisma = emolgaJSON.getJSONObject("drafts").getJSONObject("Prisma")
        val picks = prisma.getJSONObject("picks")
        val jsonList = picks.getJSONList(user.id)
        jsonList.reverse()
        e.textChannel.sendMessage("**" + user.effectiveName + "**")
            .setActionRow(Button.primary("prisma;lol", "Nächstes Pokemon"))
            .queue { m: Message ->
                prismaTeam[m.idLong] = PrismaTeam(jsonList.stream()
                    .map { o: JSONObject? -> o!!.getString("name") }
                    .toList(), prisma.getLongList("table").indexOf(user.idLong))
            }
    }
}