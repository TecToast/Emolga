package de.tectoast.emolga.database.exposed

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.embedColor
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.util.SLF4J
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import java.time.temporal.ChronoUnit

object GiveawaysDB : IntIdTable("giveaways") {
    val MESSAGEID = long("messageid")
    val CHANNELID = long("channelid")
    val HOSTID = long("hostid")
    val PRIZE = text("prize")
    val END = timestamp("end")
    val WINNERS = integer("winners")
}

class Giveaway(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Giveaway>(GiveawaysDB) {
        val logger by SLF4J
        private val coroutineScope =
            createCoroutineScope("Giveaway", Dispatchers.Default)

        /**
         * Initializes the giveaway system
         */
        suspend fun init() {
            newSuspendedTransaction {
                for (giveaway in all()) {
                    giveaway.startTimer()
                }
            }
        }
    }

    /**
     * Starts the timer of the given giveaway
     */
    fun startTimer() {
        coroutineScope.launch {
            delay(Instant.now().until(end, ChronoUnit.MILLIS))
            finish()
        }
    }

    var messageid by GiveawaysDB.MESSAGEID
    var channelid by GiveawaysDB.CHANNELID
    var hostid by GiveawaysDB.HOSTID
    var prize by GiveawaysDB.PRIZE
    var end by GiveawaysDB.END
    var winners by GiveawaysDB.WINNERS

    /**
     * Finishes the giveaway
     */
    private suspend fun finish() {
        newSuspendedTransaction {
            delete()
        }
        val tc = jda.getTextChannelById(channelid) ?: return
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
