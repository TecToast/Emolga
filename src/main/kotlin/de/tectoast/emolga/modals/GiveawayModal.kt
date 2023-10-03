package de.tectoast.emolga.modals

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.database.exposed.Giveaway
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.reply_
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

object GiveawayModal : ModalListener("gcreate") {
    override suspend fun process(e: ModalInteractionEvent, name: String?) {
        val interaction = e.interaction
        val time = interaction.getValue("time")?.asString?.let { s ->
            Command.parseShortTime(s).takeIf { it > 0 } ?: run {
                e.reply_("Das ist keine gültige Zeit!", ephemeral = true).queue()
                return
            }
        } ?: run {
            e.reply_("Du musst eine Zeit angegeben!", ephemeral = true).queue()
            return
        }
        val winners = interaction.getValue("winners")?.asString?.toIntOrNull()?.takeIf { it > 0 } ?: run {
            e.reply_("Du musst eine valide Anzahl an Gewinnern angegeben!", ephemeral = true).queue()
            return
        }
        val prize = interaction.getValue("prize")?.asString ?: run {
            e.reply_("Du musst einen Preis angegeben!", ephemeral = true).queue()
            return
        }
        e.reply_("Giveaway erstellt!", ephemeral = true).queue()
        val end = Instant.ofEpochMilli(System.currentTimeMillis() + time * 1000)
        val message = e.channel.sendMessageEmbeds(Embed {
            color = embedColor
            title = "Giveaway: $prize"
            footer("${if (winners == 1) "" else "$winners Gewinner | "}Endet")
            timestamp = end
            description =
                "Reagiere mit ${Constants.GIVEAWAY_EMOTE_MENTION} um teilzunehmen!\nGehostet von: ${interaction.user.asMention}"
        }).await()
        transaction {
            val g = Giveaway.new {
                this.end = end
                this.prize = prize
                this.winners = winners
                this.messageid = message.idLong
                this.channelid = e.channel.idLong
                this.hostid = interaction.user.idLong
            }
            message.addReaction(Emoji.fromCustom(Constants.GIVEAWAY_EMOTE_NAME, Constants.GIVEAWAY_EMOTE_ID, false))
                .queue()
            g.startTimer()
        }
    }
}
