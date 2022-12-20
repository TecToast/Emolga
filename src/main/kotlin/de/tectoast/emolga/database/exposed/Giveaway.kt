package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.*
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.temporal.ChronoUnit

object Giveaways : IdTable<Long>("giveaways") {
    val messageid = long("messageid")
    val channelid = long("channelid")
    val hostid = long("hostid")
    val prize = text("prize")
    val end = timestamp("end")
    val winners = integer("winners")
    override val id = messageid.entityId()

}

class Giveaway(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Giveaway>(Giveaways) {
        val logger by SLF4J
        private val coroutineScope =
            CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineName("Giveaway") + CoroutineExceptionHandler { _, t ->
                logger.error("ERROR IN GIVEAWAY SCOPE", t)
                Command.sendToMe("Error in giveaway scope, look in console")
            })

        fun init() {
            transaction {
                for (giveaway in all()) {
                    giveaway.startTimer()
                }
            }
        }
    }

    fun startTimer() {
        coroutineScope.launch {
            delay(Instant.now().until(end, ChronoUnit.MILLIS))
            finish()
        }
    }

    var messageid by Giveaways.messageid
    var channelid by Giveaways.channelid
    var hostid by Giveaways.hostid
    var prize by Giveaways.prize
    var end by Giveaways.end
    var winners by Giveaways.winners

    private suspend fun finish() {
        transaction {
            delete()
        }
        val tc = EmolgaMain.emolgajda.getTextChannelById(channelid) ?: return
        fun edit(msg: String) {
            tc.editMessageById(messageid, msg).queue()
        }
        tc.retrieveMessageById(messageid).await()?.let { msg ->
            val reactions = msg.reactions
            val users = mutableListOf<Long>()
            reactions.firstOrNull {
                it.emoji.runCatching { it.emoji.asCustom() }.getOrNull()?.idLong == Constants.GIVEAWAY_EMOTE_ID
            }?.let { mr ->
                mr.retrieveUsers().await().filter { !it.isBot }.forEach { users.add(it.idLong) }
            } ?: run {
                edit("Es haben sich keine Leute für das Giveaway angemeldet!")
                return
            }
            val wins = users.shuffled().take(this.winners)
            val creator = "\nGehostet von <@${this.hostid}>"
            tc.editMessageEmbedsById(messageid, Embed {
                author {
                    name = prize.ifEmpty { null }
                }
                color = embedColor
                footer("Giveaway beendet")
                tc.sendMessage(if (wins.isEmpty()) {
                    "Es konnte kein Gewinner ermittelt werden!".also { description = it + creator }
                } else if (wins.size == 1) {
                    val winner = "<@${wins[0]}>"
                    description = "Gewinner: $winner$creator"
                    "Herzlichen Glückwunsch $winner! Du hast **$prize** gewonnen!"
                } else {
                    val winners = wins.joinToString { "<@$it>" }
                    description = "Gewinner: $winners$creator"
                    "Herzlichen Glückwunsch $winners! Ihr habt **$prize** gewonnen!"
                }).queue()
            }).queue()
        }
    }
}
