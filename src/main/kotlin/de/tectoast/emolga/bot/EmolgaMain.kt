package de.tectoast.emolga.bot

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.embedColor
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.exposed.Giveaway
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.dconfigurator.DConfiguratorManager
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.components.primary
import dev.minn.jda.ktx.interactions.components.secondary
import dev.minn.jda.ktx.interactions.components.success
import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.reply_
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

var injectedJDA: JDA? = null
    get() {
        if (field == null) {
            System.getenv("DISCORDTOKEN")?.let {
                field = default(it, intents = listOf()).awaitReady()
            }
        }
        return field
    }
val jda: JDA by lazy { injectedJDA ?: emolgajda }

object EmolgaMain {

    lateinit var emolgajda: JDA
    lateinit var flegmonjda: JDA
    private val logger = LoggerFactory.getLogger(EmolgaMain::class.java)

    const val NOTEMPVERSION = true
    private val CONTROLCENTRALGENERATION: Long? = null

    @Throws(Exception::class)
    fun start() {
        val eventListeners = listOf(EmolgaListener, DConfiguratorManager)
        emolgajda = default(Command.tokens.discord) {
            //intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            intents -= GatewayIntent.MESSAGE_CONTENT
            addEventListeners(*eventListeners.toTypedArray())
            setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        }
        if (NOTEMPVERSION) {
            flegmonjda = default(Command.tokens.discordflegmon) {
                intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                addEventListeners(*eventListeners.toTypedArray())
                setMemberCachePolicy(MemberCachePolicy.ALL)
            }
        }
//        Command.tokens.discordraikou.takeIf { it != "" }?.let {
//            initializeASLCoach(default(it) {
//                intents += GatewayIntent.MESSAGE_CONTENT
//            })
//        }
//        initializeASLS11(emolgajda)
        for (jda in if (NOTEMPVERSION) listOf(emolgajda, flegmonjda) else listOf(emolgajda)) {
            EmolgaListener.registerEvents(jda)
            DConfiguratorManager.registerEvent(jda)
            jda.awaitReady()
        }
        logger.info("Discord Bots loaded!")
        CONTROLCENTRALGENERATION?.let {
            emolgajda.getTextChannelById(it)!!.sendMessageEmbeds(Embed(title = "Kontrollzentrale", color = embedColor))
                .addActionRow(
                    success("controlcentral;ej", "Emolga-JSON laden"),
                    secondary("controlcentral;saveemolgajson", "Emolga-JSON speichern"),
                    primary("controlcentral;updateslash", "Slash-Commands updaten"),
                    primary("controlcentral;updatetierlist", "Tierlist updaten"),
                    success("controlcentral;breakpoint", "Breakpoint"),
                ).queue()
        }
        //Ktor.start()
        Command.awaitNextDay()
        if (NOTEMPVERSION) flegmonjda.presence.activity = Activity.playing("mit seiner Rute")
        Database.dbScope.launch {
            Command.updatePresence()
        }/*val manager = ReactionManager(emolgajda)
        manager // BS
            .registerReaction("827608009571958806", "884567614918111233", "884564674744561684", "884565654227812364")
            .registerReaction("827608009571958806", "884567614918111233", "884564533295869962", "884565697479458826")
            .registerReaction("827608009571958806", "884567614918111233", "884565288564195348", "884565609663320086")
            .registerReaction("827608009571958806", "884567614918111233", "886748333484441650", "886746672120606771")
            .registerReaction("827608009571958806", "884567614918111233", "886748333484441650", "886746672120606771")
            .registerReaction("827608009571958806", "884567614918111233", "921389285188440115", "921387730200584222")*//*GiveawayManager.forAll {
            Giveaway(
                it.getLong("channelid"),
                it.getLong("hostid"),
                it.getTimestamp("end").toInstant(),
                it.getInt("winners"),
                it.getString("prize"),
                it.getLong("messageid")
            )
        }*/
        Giveaway.init()
    }

    @Suppress("unused")
    private fun initializeASLCoach(raikou: JDA) {
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, t ->
            logger.error("ERROR IN ASL SCOPE", t)
            Command.sendToMe("Error in asl scope, look in console")
        })
        jda.listener<SlashCommandInteractionEvent> { e ->
            if (e.name != "bet") return@listener
            val coachdata = db.aslcoach.only()
            coachdata.textChannel.let {
                if (e.channel.idLong != it) {
                    return@listener e.reply_("Dieser Command funktioniert nur im Channel <#$it>!", ephemeral = true)
                        .queue()
                }
            }
            val startbet = e.getOption("startbet")!!.asInt
            if (!startbet.validBet()) {
                return@listener e.reply_("Das ist kein gültiges Startgebot!", ephemeral = true).queue()
            }
            val slashUserId = e.user.idLong.let { if (it == Constants.FLOID) coachdata.currentCoach else it }
            val steamdata = (coachdata.teamByCoach(slashUserId) ?: run {
                return@listener e.reply_("Du bist tatsächlich kein Coach c:", ephemeral = true).queue()
            })
            if (slashUserId != coachdata.currentCoach) {
                return@listener e.reply_("Du bist nicht dran!", ephemeral = true).queue()
            }
            steamdata.pointsToSpend().let {
                if (startbet > it) {
                    return@listener e.reply_("Du kannst maximal mit $it Punkten bieten!", ephemeral = true).queue()
                }
            }
            val togain = e.getOption("player")!!.asMember!!
            if (!coachdata.isPlayer(togain)) {
                return@listener e.reply_(
                    "Dieser Trainer nimmt an dieser Season nicht als Teilnehmer teil!", ephemeral = true
                ).queue()
            }
            if (coachdata.isTaken(togain.idLong)) {
                return@listener e.reply_("Dieser Trainer ist bereits verkauft!", ephemeral = true).queue()
            }
            val level = coachdata.getLevelByMember(togain).also {
                if (it in steamdata.members) {
                    return@listener e.reply_("Du hast bereits jemanden in Stufe $it!", ephemeral = true).queue()
                }
            }
            e.reply(
                "${e.user.asMention} hat ${togain.asMention} (**Stufe $level**) für **$startbet Punkte** in den Ring geworfen!\n" + "Lasset das Versteigern beginnen!"
            ).queue()
            var maxBet: Pair<Long, Int> = slashUserId to startbet
            val countdown = AtomicInteger(coachdata.config.countdownSeconds)
            var countdownJob: Job? = null
            var finished = false
            var alreadyLaunched = false
            while (!finished) {
                val res = withTimeoutOrNull(coachdata.config.waitFor) {
                    var newbet: Int = -1
                    val me = raikou.await<MessageReceivedEvent> { event ->
                        if (event.author.isBot || event.channel.idLong != e.channel.idLong) return@await false
                        val t = coachdata.teamByCoach(event.author.idLong)
                        val nbet = event.message.contentDisplay.toIntOrNull() ?: -1
                        if (t == null || !nbet.validBet() || nbet <= maxBet.second || (t.pointsToSpend() < nbet).also {
                                if (it) {
                                    logger.info("${event.member!!.effectiveName} wanted to bid $nbet, but only has ${t.pointsToSpend()} points!")
                                    Command.sendToUser(
                                        event.author.idLong,
                                        "Du hast nicht mehr genug Punkte, um mit $nbet Punkten zu bieten!"
                                    )
                                }
                            } || (level in t.members).also {
                                if (it) Command.sendToUser(
                                    event.author.idLong,
                                    "Du kannst hier nicht mitbieten, da du bereits einen Sklaven aus Stufe $level hast, du Kek! (Henny wollte, dass ich das so schreibe)"
                                )
                            }) {
                            event.message.delete().queue()
                            return@await false
                        }
                        newbet = nbet
                        true
                        //&& event.member!!.roles.any { it.idLong == 998164505529950258 }
                    }
                    if (!finished) {
                        countdownJob?.cancelAndJoin()
                        alreadyLaunched = false
                        countdown.set(coachdata.config.countdownSeconds)
                        maxBet = me.author.idLong to newbet
                    }
                }
                logger.info("WithTimeout returns $res")

                if (res == null && !alreadyLaunched) {
                    alreadyLaunched = true
                    countdownJob = scope.launch {
                        while (countdown.get() > 0) {
                            val get = countdown.getAndDecrement()
                            if (get in coachdata.config.sendOn) e.channel.sendMessage("$get Sekunde${if (get != 1) "n" else ""}...")
                                .queue()
                            delay(1000)
                        }
                        withContext(NonCancellable) {
                            finished = true
                            e.channel.sendMessage("${togain.asMention} gehört jetzt <@${maxBet.first}>, welcher für **${maxBet.second} Punkte** einen neuen Menschen versklavt hat!")
                                .queue()
                            coachdata.addUserToTeam(togain, maxBet.first, maxBet.second)
                            delay(5000)
                            coachdata.nextCoach()
                            coachdata.save()
                        }
                    }
                }
            }
        }
    }

    private fun Int.validBet() = this > 0 && this % 100 == 0
}
