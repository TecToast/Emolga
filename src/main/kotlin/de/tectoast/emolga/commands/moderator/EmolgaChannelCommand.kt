package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.Emolga

class EmolgaChannelCommand : Command(
    "emolgachannel",
    "Added/Removed einen Channel, in dem Emolga benutzt werden kann",
    CommandCategory.Moderator
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add(
                "action", "Aktion", "Die Aktion, die du durchführen möchtest",
                ArgumentManagerTemplate.Text.of(
                    SubCommand.of("add", "Fügt einen Channel hinzu"),
                    SubCommand.of("remove", "Removed einen Channel")
                )
            )
            .add(
                "channel",
                "Channel",
                "Der Channel, der geaddet/removed werden soll",
                ArgumentManagerTemplate.DiscordType.CHANNEL
            )
            .setExample("!emolgachannel add #botchannel")
            .build()
    }

    override fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val action = args.getText("action")
        val tc = args.getChannel("channel")
        val ec = Emolga.get.emolgachannel
        val gid = e.guild.idLong
        val tid = tc.idLong
        val arr = ec.getOrPut(gid) { mutableListOf() }
        if (action == "add") {
            if (tid in arr) {
                e.reply(tc.asMention + " wurde bereits als Channel eingestellt!")
                return
            }
            arr += tid
            emolgaChannel.getOrPut(gid) { mutableListOf() }.add(tid)
            saveEmolgaJSON()
            e.reply("Der Channel " + tc.asMention + " wurde erfolgreich zu den erlaubten Channeln hinzugefügt!")
        } else {
            if (arr.size == 0) {
                e.reply("Auf diesem Server wurden noch keine Channel für mich eingestellt!")
                return
            }
            if (arr.remove(tid)) {
                e.reply(tc.asMention + " wurde erfolgreich aus den erlaubten Channeln gelöscht!")
                emolgaChannel[gid]!!.remove(tid)
                saveEmolgaJSON()
                return
            }
            e.reply(tc.asMention + " ist nicht in der Liste der erlaubten Channel!")
        }
    }
}