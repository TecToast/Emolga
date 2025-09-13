package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.ifTrue
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import dev.minn.jda.ktx.events.await
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.atomic.AtomicInteger

object BetCommand :
    CommandFeature<BetCommand.Args>(
        ::Args,
        CommandSpec("bet", "Beginne die Versteigerung eines Spielers")
    ) {
    class Args : Arguments() {
        var player by member("Spieler", "Der Spieler, der versteigert werden soll")
        var startBet by int("Startgebot", "Das Startgebot für den Spieler (muss über 0 und durch 100 teilbar sein)")
    }

    private val logger = KotlinLogging.logger {}

    val scope = createCoroutineScope("ASLCoach")

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val coachdata = db.aslcoach.only()
        coachdata.textChannel.let {
            if (iData.tc != it) {
                return iData.reply("Dieser Command funktioniert nur im Channel <#$it>!", ephemeral = true)
            }
        }
        val startbet = e.startBet
        if (!startbet.validBet()) {
            return iData.reply("Das ist kein gültiges Startgebot!", ephemeral = true)
        }
        val bettingCoach = when {
            iData.user == Constants.FLOID -> coachdata.currentCoach
            else -> iData.user
        }
        val steamdata = (coachdata.teamByCoach(bettingCoach) ?: run {
            return iData.reply("Du bist tatsächlich kein Coach c:", ephemeral = true)
        })
        if (bettingCoach != coachdata.currentCoach) {
            return iData.reply("Du bist nicht dran!", ephemeral = true)
        }
        steamdata.pointsToSpend(coachdata).let {
            if (startbet > it) {
                return iData.reply("Du kannst maximal mit $it Punkten bieten!", ephemeral = true)
            }
        }
        val togain = e.player
        if (!coachdata.isPlayer(togain)) {
            return iData.reply(
                "Dieser Trainer ist nicht zur Auktion freigegeben!", ephemeral = true
            )
        }
        if (coachdata.isTaken(togain.idLong)) {
            return iData.reply("Dieser Trainer ist bereits verkauft!", ephemeral = true)
        }
        val level = coachdata.getLevelByMember(togain).also {
            if (it in steamdata.members) {
                return iData.reply("Du hast bereits jemanden in Stufe $it!", ephemeral = true)
            }
        }
        iData.reply(
            "<@${iData.user}> hat ${togain.asMention} (**Stufe $level**) für **$startbet Punkte** in den Ring geworfen!\n" + "Lasset das Versteigern beginnen!"
        )
        var maxBet: Pair<Long, Int> = bettingCoach to startbet
        val countdown = AtomicInteger(coachdata.config.countdownSeconds)
        var countdownJob: Job? = null
        var finished = false
        var alreadyLaunched = false
        while (!finished) {
            val res = withTimeoutOrNull(coachdata.config.waitFor) {
                var newbet: Int = -1
                val me = EmolgaMain.raikoujda!!.await<MessageReceivedEvent> { event ->
                    if (event.author.isBot || event.channel.idLong != iData.tc) return@await false
                    val t = coachdata.teamByCoach(event.author.idLong)
                    val nbet = event.message.contentDisplay.toIntOrNull() ?: -1
                    val pointsToSpend = t?.pointsToSpend(coachdata)
                    if (
                        t == null ||
                        !nbet.validBet() ||
                        nbet <= maxBet.second ||
                        (pointsToSpend!! < nbet).ifTrue {
                            logger.info("${event.member!!.effectiveName} wanted to bid $nbet, but only has $pointsToSpend points!")
                            SendFeatures.sendToUser(
                                event.author.idLong,
                                "Du hast nicht mehr genug Punkte, um mit $nbet Punkten zu bieten!"
                            )
                        } ||
                        (level in t.members).ifTrue {
                            SendFeatures.sendToUser(
                                event.author.idLong,
                                "Du kannst hier nicht mitbieten, da du bereits einen Sklaven aus Stufe $level hast!"
                            )
                        }
                    ) {
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
                        if (get in coachdata.config.sendOn) iData.textChannel.sendMessage("$get Sekunde${if (get != 1) "n" else ""}...")
                            .queue()
                        delay(1000)
                    }
                    withContext(NonCancellable) {
                        finished = true
                        val buyer = maxBet.first
                        val points = maxBet.second
                        iData.textChannel.sendMessage(
                            "${togain.asMention} wurde für **$points Punkte** an <@$buyer> [${
                                coachdata.teamnameByCoach(
                                    buyer
                                )
                            }] verkauft!"
                        )
                            .queue()
                        coachdata.addUserToTeam(togain, buyer, maxBet.second)
                        delay(5000)
                        coachdata.nextCoach()
                        coachdata.save()
                    }
                }
            }

        }
    }

    private fun Int.validBet() = this > 0 && this % 100 == 0
}
