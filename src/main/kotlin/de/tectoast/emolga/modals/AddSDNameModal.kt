package de.tectoast.emolga.modals

import de.tectoast.emolga.database.exposed.SDInsertStatus
import de.tectoast.emolga.database.exposed.SDNamesDB
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent

object AddSDNameModal : ModalListener("addsdname") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val sdname = e.getValue("name")!!.asString
        val parsed = e.getValue("id")?.asString?.toLongOrNull()
        val idSpecified = parsed != null
        val specifiedName = if (idSpecified) e.jda.retrieveUserById(parsed!!).await().name else e.user.name
        val id = parsed ?: e.user.idLong
        when (SDNamesDB.addIfAbsent(sdname, id).await()) {
            SDInsertStatus.SUCCESS -> {
                if (idSpecified) {
                    e.reply("Der Name `$name` wurde für $specifiedName registriert!")
                } else {
                    e.reply("Der Name `$name` wurde für dich registriert!")
                }
            }

            SDInsertStatus.ALREADY_OWNED_BY_YOU -> {
                e.reply("Der Name `$name` gehört bereits ${if (idSpecified) specifiedName else "dir"}!")
            }

            SDInsertStatus.ALREADY_OWNED_BY_OTHER -> {
                e.reply("Der Name ist bereits von jemand anderem belegt! Es wird geprüft, ob dieser Name freigegeben werden kann.")
            }
        }
    }
}
