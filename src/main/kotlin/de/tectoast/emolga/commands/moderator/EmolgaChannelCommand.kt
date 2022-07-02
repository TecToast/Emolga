package de.tectoast.emolga.commands.moderator

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.jsolf.JSONArray

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
        val args = e.arguments!!
        val action = args.getText("action")
        val tc = args.getChannel("channel")
        val ec = emolgaJSON.getJSONObject("emolgachannel")
        val gid = e.guild.id
        val tid = tc.idLong
        if (!ec.has(gid)) ec.put(gid, JSONArray())
        val arr = ec.getJSONArray(gid)
        val l = arr.toLongList()
        val gidl = e.guild.idLong
        if (action == "add") {
            if (l.contains(tid)) {
                e.reply(tc.asMention + " wurde bereits als Channel eingestellt!")
                return
            }
            arr.put(tid)
            if (!emolgaChannel.containsKey(gidl)) emolgaChannel[gidl] = ArrayList()
            emolgaChannel[gidl]!!.add(tid)
            saveEmolgaJSON()
            e.reply("Der Channel " + tc.asMention + " wurde erfolgreich zu den erlaubten Channeln hinzugefügt!")
        } else {
            if (arr.length() == 0) {
                e.reply("Auf diesem Server wurden noch keine Channel für mich eingestellt!")
                return
            }
            if (removeFromJSONArray(arr, tid)) {
                e.reply(tc.asMention + " wurde erfolgreich aus den erlaubten Channeln gelöscht!")
                emolgaChannel[gidl]!!.remove(tid)
                saveEmolgaJSON()
                return
            }
            e.reply(tc.asMention + " ist nicht in der Liste der erlaubten Channel!")
        }
    }
}