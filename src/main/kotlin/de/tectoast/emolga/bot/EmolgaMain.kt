package de.tectoast.emolga.bot

import de.tectoast.emolga.bot.EmolgaMain.emolgajda
import de.tectoast.emolga.encryption.Credentials
import de.tectoast.emolga.features.FeatureManager
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.dconfigurator.DConfiguratorManager
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.emolga.getCount
import de.tectoast.emolga.utils.json.only
import de.tectoast.emolga.utils.marker
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
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag.*
import org.litote.kmongo.eq
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

object EmolgaMain : CoroutineScope by createCoroutineScope("EmolgaMain") {
    const val BOT_DISABLED = false
    const val DISABLED_TEXT =
        "Es finden derzeit große interne Umstrukturierungen statt, ich werde voraussichtlich heute Mittag/Nachmittag wieder einsatzbereit sein :)"

    lateinit var emolgajda: JDA
    lateinit var flegmonjda: JDA
    private val logger = LoggerFactory.getLogger(EmolgaMain::class.java)

    val featureManager = OneTimeCache { FeatureManager("de.tectoast.emolga.features") }

    fun launchBots() {
        Message.suppressContentIntentWarning()
        emolgajda = default(Credentials.tokens.discord) {
            //intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            intents -= GatewayIntent.MESSAGE_CONTENT
            setMemberCachePolicy(MemberCachePolicy.DEFAULT)
        }
        emolgajda.listener<ReadyEvent> {
            logger.info("important".marker, "Emolga is now online!")
            db.drafts.find(League::isRunning eq true).toFlow().collect {
                if (it.noAutoStart) return@collect
                logger.info("important".marker, "Starting draft ${it.leaguename}...")
                it.startDraft(null, true, null)
            }
        }
        flegmonjda = default(Credentials.tokens.discordflegmon) {
            intents += listOf(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
            setMemberCachePolicy(MemberCachePolicy.ALL)
        }
//        Command.tokens.discordraikou.takeIf { it != "" }?.let {
//            initializeASLCoach(default(it) {
//                intents += GatewayIntent.MESSAGE_CONTENT
//            })
//        }
//        initializeASLS11(emolgajda)
    }

    @Throws(Exception::class)
    fun startListeners() {
        launch {
            featureManager.updateCachedValue()
        }
        for (jda in listOf(emolgajda, flegmonjda)) {
            jda.listener<GenericEvent> {
                featureManager().handleEvent(it)
            }
            DConfiguratorManager.registerEvent(jda)
            jda.awaitReady()
        }
        logger.info("Discord Bots loaded!")
        ControlButtonSetup.init()
        flegmonjda.presence.activity = Activity.playing("mit seiner Rute")
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
