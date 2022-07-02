package de.tectoast.emolga.commands.various

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.Giveaway
import de.tectoast.toastilities.interactive.ErrorMessage
import de.tectoast.toastilities.interactive.Interactive
import de.tectoast.toastilities.interactive.InteractiveTemplate
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class GcreateCommand : Command(
    "gcreate",
    "Startet ein Giveaway",
    CommandCategory.Various,
    712035338846994502L,
    756239772665511947L,
    518008523653775366L,
    673833176036147210L,
    745934535748747364L,
    821350264152784896L
) {
    private val current: MutableSet<String> = HashSet()
    private val template: InteractiveTemplate

    //GuildMessageReceivedEvent event, TextChannel tchan, int seconds, int winners
    init {
        argumentTemplate = ArgumentManagerTemplate.noArgs()
        template = InteractiveTemplate({ u: User, tco: MessageChannel, l: LinkedHashMap<String?, Any?> ->
            current.remove(tco.id)
            val tchan = l["channel"] as TextChannel?
            val seconds = l["seconds"] as Int
            val winners = l["winners"] as Int
            val prize = l["prize"] as String?
            val now = Instant.now()
            val end = now.plusSeconds(seconds.toLong())
            val g = Giveaway(tchan!!.idLong, u.idLong, end, winners, prize)
            val message = g.render(now)
            tchan.sendMessage(message).queue { m: Message ->
                m.addReaction(tco.jda.getEmoteById("772191611487780934")!!).queue()
                g.messageId = m.idLong
                tco.sendMessage("Das Giveaway wurde erstellt!").queue()
            }
        }, CANCEL)
            .addLayer(
                "channel",
                "Es geht los! Zuerst, in welchem Channel soll das Giveaway stattfinden?$CHANNEL",
                { m: Message ->
                    val channels = m.mentions.getChannels(
                        TextChannel::class.java
                    )
                    if (channels.size != 1) {
                        return@addLayer ErrorMessage("Du musst einen Channel taggen!")
                    }
                    channels[0]
                }) { o: Any -> (o as TextChannel).asMention }
            .addLayer(
                "seconds",
                "Das Giveaway wird in {channel} stattfinden! Wie lange soll das Giveaway laufen?$TIME",
                { m: Message ->
                    val seconds = parseShortTime(m.contentRaw)
                    if (seconds == -1) {
                        return@addLayer ErrorMessage("Das ist keine valide Zeitangabe!")
                    }
                    seconds
                }) { o: Any -> secondsToTime((o as Int).toLong()) }
            .addLayer(
                "winners",
                "Okay! Das Giveaway dauert {seconds}! Wieviele Gewinner soll es geben?$WINNERS"
            ) { m: Message ->
                try {
                    return@addLayer m.contentRaw.trim().toInt()
                } catch (ex: NumberFormatException) {
                    return@addLayer ErrorMessage("Das ist keine valide Zahl.")
                }
            }
            .addLayer(
                "prize",
                "Okay! {winners} Gewinner! Zum Schluss: Was möchtest du giveawayen?$PRIZE"
            ) { m: Message ->
                val prize = m.contentRaw
                if (prize.length > 250) {
                    return@addLayer ErrorMessage("Der Preis ist zu lang! Bitte wähle einen kürzeren aus!$PRIZE")
                }
                prize
            }
        CANCEL_WORDS.forEach(Consumer { cmd: String? -> template.addCancelCommand(cmd) })
        template.setTimer(
            2,
            TimeUnit.MINUTES,
            "Du hast länger als 2 Minuten für eine Antwort gebraucht, deshalb wurde das Giveaway gelöscht."
        )
        template.setOnCancel { i: Interactive -> current.remove(i.tco.id) }
    }

    override fun process(e: GuildCommandEvent) {
        val tco = e.textChannel
        if (current.contains(tco.id)) return
        current.add(tco.id)
        template.createInteractive(e.author, tco, e.message!!.idLong)
    }

    companion object {
        private const val CANCEL = "Die Erstellung des Giveaways wurde abgebrochen."
        private const val CHANNEL = "\n\n`Tagge bitte einen Channel.`"
        private const val TIME = "\n\n`Bitte gib die Länge des Giveaways an.`"
        private const val WINNERS = "\n\n`Bitte gib die Anzahl der Gewinner ein.`"
        private const val PRIZE = "\n\n`Bitte gib den Preis ein. Dies wird ebenfalls das Giveaway starten.`"
        private val CANCEL_WORDS = listOf("cancel", "!gcancel", "g!cancel")
    }
}