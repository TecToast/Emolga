package de.tectoast.emolga.utils

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.commands.Command.Companion.secondsToTime
import de.tectoast.emolga.utils.sql.managers.GiveawayManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.entities.emoji.CustomEmoji
import net.dv8tion.jda.api.entities.emoji.Emoji
import org.slf4j.LoggerFactory
import java.awt.Color
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Giveaway {
    val end: Instant
    val winners: Int
    val prize: String?
    val channelId: Long
    val userId: Long
    var messageId: Long = -1
    private var isEnded = false

    constructor(channelId: Long, userId: Long, end: Instant, winners: Int, prize: String?) {
        this.channelId = channelId
        this.userId = userId
        this.end = end
        this.winners = winners
        this.prize = if (prize == null) null else if (prize.isEmpty()) null else prize
        saveToDB()
    }

    constructor(channelId: Long, userId: Long, end: Instant, winners: Int, prize: String?, mid: Long) {
        messageId = mid
        this.channelId = channelId
        this.userId = userId
        this.end = end
        this.winners = winners
        this.prize = if (prize == null) null else if (prize.isEmpty()) null else prize
        logger.info((end.toEpochMilli() - System.currentTimeMillis()).toString())
        giveawayFutures[mid] = giveawayExecutor.scheduleAtFixedRate({
            val now = Instant.now()
            if (now.until(end, ChronoUnit.MILLIS) < 6000) {
                giveawayFinalizes[mid] = giveawayExecutor.scheduleAtFixedRate(
                    inner@{
                        if (now.until(end, ChronoUnit.MILLIS) < 1000) {
                            end()
                            giveawayFinalizes[mid]!!.cancel(false)
                            giveawayFinalizes.remove(mid)
                            return@inner
                        }
                        render(now)
                    }, 0, 1, TimeUnit.SECONDS
                )
                giveawayFutures[mid]!!.cancel(false)
                giveawayFutures.remove(mid)
                return@scheduleAtFixedRate
            }
            render(now)
        }, 0, 5000, TimeUnit.MILLISECONDS)
    }

    private fun saveToDB() {
        GiveawayManager.saveGiveaway(this)
    }

    fun render(now: Instant): Message {
        val mb = MessageBuilder()
        val close = now.plusSeconds(9).isAfter(end)
        mb.append("\uD83C\uDF89").append(if (close) " **G I V E A W A Y** " else "   **GIVEAWAY**   ")
            .append("\uD83C\uDF89")
        val eb = EmbedBuilder()
        eb.setColor(Color.CYAN)
        eb.setFooter((if (winners == 1) "" else "$winners Gewinner | ") + "Endet", null)
        eb.setTimestamp(end)
        eb.setDescription(
            """Reagiere mit ${Constants.APFELKUCHENMENTION} um dem Giveaway beizutreten!
Verbleibende Zeit: ${secondsToTime(now.until(end, ChronoUnit.SECONDS))}
Gehostet von: <@$userId>"""
        )
        if (prize != null) eb.setAuthor(prize, null, null)
        if (close) eb.setTitle("Letzte Chance!!!", null)
        mb.setEmbeds(eb.build())
        return mb.build()
    }

    private fun end() {
        isEnded = true
        GiveawayManager.removeGiveaway(this)
        val mb = MessageBuilder()
        mb.append("\uD83C\uDF89").append(" **GIVEAWAY ZU ENDE** ").append("\uD83C\uDF89")
        val eb = EmbedBuilder()
        eb.setColor(Color.CYAN) // dark theme background
        eb.setFooter((if (winners == 1) "" else "$winners Gewinner | ") + "Endete", null)
        eb.setTimestamp(end)
        if (prize != null) eb.setAuthor(prize, null, null)
        try {
            val opt: MessageReaction? = emolgajda.getTextChannelById(channelId)?.retrieveMessageById(messageId)
                ?.complete()?.reactions?.let { mr ->
                    mr.firstOrNull { it.emoji.type == Emoji.Type.CUSTOM && (it.emoji as CustomEmoji).idLong == 967390962877870090 }
                }
            val members = ArrayList<Long>()
            opt?.run {
                retrieveUsers().complete().filter { !it.isBot && it.idLong != userId }
            }
            val wins = ArrayList<Long>()
            if (members.size > 0) for (i in 0 until winners) {
                if (members.size == 0) break
                wins.add(members.removeAt(Random().nextInt(members.size)))
            }
            //restJDA.getReactionUsers(channelId, messageId, EncodingUtil.encodeUTF8(Constants.TADA))..submit().thenAcceptAsync(ids -> {
            //List<Long> wins = GiveawayUtil.selectWinners(ids, winners);
            val toSend: String = if (wins.isEmpty()) {
                eb.setDescription("Es konnte kein Gewinner ermittelt werden!")
                "Es konnte kein Gewinner ermittelt werden!"
            } else if (wins.size == 1) {
                eb.setDescription("Gewinner: <@${wins[0]}>")
                "Herzlichen Glückwunsch <@${wins[0]}>! Du hast${if (prize == null) "" else " **$prize**"} gewonnen!"
            } else {
                eb.setDescription("Gewinner:")
                wins.forEach { eb.appendDescription("\n<@").appendDescription(it.toString()).appendDescription(">") }
                "Herzlichen Glückwunsch ${wins.joinToString { "<@$it>" }}! Ihr habt${if (prize == null) "" else " **$prize**"} gewonnen!"
            }
            mb.setEmbeds(eb.appendDescription("\nGehostet von: <@$userId>").build())
            emolgajda.getTextChannelById(channelId)?.editMessageById(messageId, mb.build())?.queue()
            emolgajda.getTextChannelById(channelId)?.sendMessage(toSend)?.queue()
        } catch (e: Exception) {
            e.printStackTrace()
            mb.setEmbeds(
                eb.setDescription("Es konnte kein Gewinner festgestellt werden!\nGehostet von: <@$userId>").build()
            )
            emolgajda.getTextChannelById(channelId)?.editMessageById(messageId, mb.build())?.queue()
            emolgajda.getTextChannelById(channelId)?.sendMessage("Es konnte kein Gewinner festgestellt werden!")
                ?.queue()
        }
    }

    override fun toString(): String {
        return "Giveaway{" + "end=" + end +
                ", winners=" + winners +
                ", prize='" + prize + '\'' +
                ", channelId='" + channelId + '\'' +
                ", userId='" + userId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", isEnded=" + isEnded +
                '}'
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Giveaway::class.java)
        private val giveawayExecutor: ScheduledExecutorService = ScheduledThreadPoolExecutor(5)
        private val giveawayFutures = HashMap<Long, ScheduledFuture<*>>()
        private val giveawayFinalizes = HashMap<Long, ScheduledFuture<*>>()
    }
}