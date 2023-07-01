package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.json.EmolgaChannelConfig
import de.tectoast.emolga.utils.json.db
import org.litote.kmongo.addToSet
import org.litote.kmongo.eq
import org.litote.kmongo.pull

class EmolgaChannelCommand : Command(
    "emolgachannel", "Added/Removed einen Channel, in dem Emolga benutzt werden kann", CommandCategory.Moderator
) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder().add(
            "action", "Aktion", "Die Aktion, die du durchführen möchtest", ArgumentManagerTemplate.Text.of(
                SubCommand.of("add", "Fügt einen Channel hinzu"), SubCommand.of("remove", "Removed einen Channel")
            )
        ).add(
            "channel",
            "Channel",
            "Der Channel, der geaddet/removed werden soll",
            ArgumentManagerTemplate.DiscordType.CHANNEL
        ).setExample("!emolgachannel add #botchannel").build()
    }

    override suspend fun process(e: GuildCommandEvent) {
        val args = e.arguments
        val action = args.getText("action")
        val tc = args.getChannel("channel")
        val ec = db.emolgachannel
        val gid = e.guild.idLong
        val tid = tc.idLong
        val config = ec.findOne(EmolgaChannelConfig::guild eq gid) ?: EmolgaChannelConfig(gid, mutableListOf())
        val arr = config.channels
        if (action == "add") {
            if (tid in arr) {
                e.reply(tc.asMention + " wurde bereits als Channel eingestellt!")
                return
            }
            ec.updateOne(EmolgaChannelConfig::guild eq gid, addToSet(EmolgaChannelConfig::channels, tid))
            e.reply("Der Channel " + tc.asMention + " wurde erfolgreich zu den erlaubten Channeln hinzugefügt!")
        } else {
            if (arr.isEmpty()) {
                e.reply("Auf diesem Server wurden noch keine Channel für mich eingestellt!")
                return
            }

            if (ec.updateOne(
                    EmolgaChannelConfig::guild eq gid, pull(EmolgaChannelConfig::channels, tid)
                ).modifiedCount > 0
            ) {
                return e.reply(tc.asMention + " wurde erfolgreich aus den erlaubten Channeln gelöscht!")
            }
            e.reply(tc.asMention + " ist nicht in der Liste der erlaubten Channel!")
        }
    }
}
