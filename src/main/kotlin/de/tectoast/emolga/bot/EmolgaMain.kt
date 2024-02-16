package de.tectoast.emolga.bot

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.database.Database
import de.tectoast.emolga.database.exposed.Giveaway
import de.tectoast.emolga.encryption.Credentials
import de.tectoast.emolga.features.FeatureManager
import de.tectoast.emolga.features.flegmon.BirthdaySystem
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.features.various.ControlCentralButton
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.dconfigurator.DConfiguratorManager
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.getCount
import de.tectoast.emolga.utils.json.only
import dev.minn.jda.ktx.events.await
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.jdabuilder.cache
import dev.minn.jda.ktx.jdabuilder.default
import dev.minn.jda.ktx.jdabuilder.intents
import dev.minn.jda.ktx.messages.*
import kotlinx.coroutines.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag.*
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

var injectedJDA: JDA? = null
    get() {
        if (field == null) {
            System.getenv("DISCORDTOKEN")?.let {
                usedJDA = true
                field = default(it, intents = listOf()) {
                    cache -= listOf(VOICE_STATE, EMOJI, STICKER, SCHEDULED_EVENTS)
                }.awaitReady()
            }
        }
        return field
    }
var usedJDA = false
    private set
val jda: JDA by lazy { injectedJDA ?: emolgajda }

object EmolgaMain {
    const val BOT_DISABLED = false
    const val DISABLED_TEXT =
        "Es finden derzeit große interne Umstrukturierungen statt, ich werde voraussichtlich heute Mittag/Nachmittag wieder einsatzbereit sein :)"

    lateinit var emolgajda: JDA
    lateinit var flegmonjda: JDA
    private val logger = LoggerFactory.getLogger(EmolgaMain::class.java)

    const val NOTEMPVERSION = true
    private val CONTROLCENTRALGENERATION: Pair<Long, Long?> = 967890099029278740 to 967890640065134602
    val featureManager = FeatureManager("de.tectoast.emolga.features")

    @Throws(Exception::class)
    suspend fun start() {
        val eventListeners = listOf(DConfiguratorManager)
        emolgajda = default(Credentials.tokens.discord) {
            //intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            intents -= GatewayIntent.MESSAGE_CONTENT
            addEventListeners(*eventListeners.toTypedArray())
            setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        }
        if (NOTEMPVERSION) {
            flegmonjda = default(Credentials.tokens.discordflegmon) {
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
            jda.listener<GenericEvent> {
                featureManager.handleEvent(it)
            }
            DConfiguratorManager.registerEvent(jda)
            jda.awaitReady()
        }
        logger.info("Discord Bots loaded!")
        CONTROLCENTRALGENERATION.let {
            val tc = emolgajda.getTextChannelById(it.first)!!
            val embed = Embed(title = "Kontrollzentrale", color = embedColor).into()
            val components = listOf(
                ControlCentralButton("Slash-Commands updaten", ButtonStyle.PRIMARY) {
                    mode = ControlCentralButton.Mode.UPDATE_SLASH
                },
                ControlCentralButton("Tierlist updaten", ButtonStyle.PRIMARY) {
                    mode = ControlCentralButton.Mode.UPDATE_TIERLIST
                },
                ControlCentralButton("Breakpoint", ButtonStyle.SUCCESS) { mode = ControlCentralButton.Mode.BREAKPOINT },
            ).into()
            it.second?.let { mid ->
                tc.editMessage(
                    mid.toString(), embeds = embed, components = components
                ).queue()
            } ?: tc.send(embeds = embed, components = components).queue()
        }
        //Ktor.start()
        BirthdaySystem.startSystem()
        if (NOTEMPVERSION) flegmonjda.presence.activity = Activity.playing("mit seiner Rute")
        Database.dbScope.launch {
            updatePresence()
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

    suspend fun updatePresence() {
        if (BOT_DISABLED) {
            emolgajda.presence.setPresence(
                OnlineStatus.DO_NOT_DISTURB, Activity.watching("auf den Wartungsmodus")
            )
            return
        }
        val count = db.statistics.getCount("analysis")
        if (count % 100 == 0) {
            emolgajda.getTextChannelById(904481960527794217L)!!
                .sendMessage(SimpleDateFormat("dd.MM.yyyy").format(Date()) + ": " + count).queue()
        }
        emolgajda.presence.setPresence(OnlineStatus.ONLINE, Activity.watching("auf $count analysierte Replays"))
    }

    @Suppress("unused")
    private fun initializeASLCoach(raikou: JDA) {
        val scope = createCoroutineScope("ASLCoach")
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
                                    SendFeatures.sendToUser(
                                        event.author.idLong,
                                        "Du hast nicht mehr genug Punkte, um mit $nbet Punkten zu bieten!"
                                    )
                                }
                            } || (level in t.members).also {
                                if (it) SendFeatures.sendToUser(
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
