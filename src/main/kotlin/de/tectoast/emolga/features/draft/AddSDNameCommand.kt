package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.database.exposed.SDInsertStatus
import de.tectoast.emolga.database.exposed.SDNamesDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import dev.minn.jda.ktx.coroutines.await

object AddSDNameCommand : CommandFeature<AddSDNameCommand.Args>(
    ::Args,
    CommandSpec("addsdname", "Registriert deinen Showdown-Namen bei Emolga")
) {
    class Args : Arguments() {
        var name by string("SD-Name", "Dein Showdown-Name")
        var id by long("Die ID (nur Flo)", "Nur für Flo").nullable()
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        val idArg = e.id
        val idSpecified = idArg != null
        if (idSpecified && isNotFlo) {
            return reply("Nur Flo darf den Command mit einer ID verwenden!")
        }
        val name = e.name
        val id = if (idSpecified) idArg!! else user
        val specifiedName = if (idSpecified) jda.retrieveUserById(id).await().name else user().name
        when (SDNamesDB.addIfAbsent(name, id).await()) {
            SDInsertStatus.SUCCESS -> {
                if (idSpecified) {
                    reply("Der Name `$name` wurde für $specifiedName registriert!")
                } else {
                    reply("Der Name `$name` wurde für dich registriert!")
                }
            }

            SDInsertStatus.ALREADY_OWNED_BY_YOU -> {
                reply("Der Name `$name` gehört bereits ${if (idSpecified) specifiedName else "dir"}!")
            }

            SDInsertStatus.ALREADY_OWNED_BY_OTHER -> {
                reply("Der Name ist bereits von jemand anderem belegt! Es wird geprüft, ob dieser Name freigegeben werden kann.")
            }
        }
    }
}
