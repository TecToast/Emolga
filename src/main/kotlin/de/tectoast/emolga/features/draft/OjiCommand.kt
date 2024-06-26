package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.OneTimeCache
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.interactions.components.ActionRow
import org.litote.kmongo.eq
import org.litote.kmongo.regex

object Oji {
    object OjiCommand : CommandFeature<NoArgs>(
        NoArgs(),
        CommandSpec("oji", "Gibt aus, welche Kämpfe schon beendet sind und welche nicht.", Constants.G.VIP)
    ) {
        init {
            slashPrivate()
        }

        fun Arguments.leagueName() = fromList(
            "Liganame",
            "Der Name der Liga, die du überprüfen willst.",
            { event -> db.drafts.find(League::guild eq event.guild?.idLong).toList().map { it.leaguename } }) {
        }

        object ResultCheckCommand : CommandFeature<ResultCheckCommand.Args>(
            ::Args,
            CommandSpec("resultcheck", "Gibt aus, welche Kämpfe schon beendet sind und welche nicht."),
        ) {
            class Args : Arguments() {
                var league by leagueName()
                var gameday by int("Gameday", "Der Spieltag, den du überprüfen willst.")
                var public by boolean("Public", "Soll die Nachricht öffentlich sein?") {
                    default = false
                }
            }

            context(InteractionData)
            override suspend fun exec(e: Args) {
                val league = db.getLeague(e.league) ?: return reply("Liga nicht gefunden!", ephemeral = true)
                reply(league.buildStoreStatus(e.gameday), ephemeral = !e.public)
            }

        }

        object VoteMenus : CommandFeature<VoteMenus.Args>(::Args, CommandSpec("votemenus", "Sendet die Vote-Menus")) {
            class Args : Arguments() {
                var league by leagueName()
            }

            context(InteractionData)
            override suspend fun exec(e: Args) {
                val league = db.getLeague(e.league) ?: return reply("Liga nicht gefunden!", ephemeral = true)
                done(true)
                textChannel.send(components = (1..3).map {
                    ActionRow.of(
                        TipGameManager.RankSelect.createFromLeague(
                            league,
                            it
                        )
                    )
                }).queue()
            }
        }

        object TopKillerGuess : CommandFeature<TopKillerGuess.Args>(
            ::Args,
            CommandSpec("topkillerguess", "Startet die Umfrage für den Top-Killer-Guess")
        ) {
            class Args : Arguments() {
                var league by leagueName()
            }

            context(InteractionData)
            override suspend fun exec(e: Args) {
                done(true)
                textChannel.send(
                    embeds = Embed(title = "Top-Killer-Guess", color = embedColor).into(),
                    components = MostKillsButton("Eintragen") { this.league = e.league }.into()
                ).queue()
            }
        }

        private val nameMap = OneTimeCache {
            jda.getGuildById(Constants.G.VIP)!!
                .retrieveMembersByIds(db.league("IPLS4L1").table + db.league("IPLS4L2").table).await()
                .associate { it.idLong to it.user.effectiveName }
        }
        private val leagueOrga = setOf(Constants.M.OJI, Constants.M.TARI, 298458563469246464, Constants.FLOID)
        private val alwaysIncluded = setOf(720387214739111968)
        context(InteractionData)
        private suspend fun executeTipGameState(withoutAdmin: Boolean) {
            deferReply(true)
            val dataList = db.tipgameuserdata.find(TipGameUserData::league regex "^IPL").toList()
            val leagues = dataList.partition { it.league == "IPLS4L1" }.toList()
            val names = nameMap()
            val points =
                dataList.groupBy { it.user }
                    .filterKeys { it in alwaysIncluded || (!withoutAdmin || (it !in names && it !in leagueOrga)) }
                    .mapValues { it.value.sumOf { d -> d.correctGuesses.values.sumOf { l -> l.size } + (if (d.correctTopkiller) 3 else 0) + d.correctOrderGuesses.sumOf { co -> 4 - co } } }
            reply(
                buildString {
                    append("Teilnehmeranzahl: ${points.size}\n\n")
                    append(points.entries.sortedByDescending { it.value }.take(10)
                        .mapIndexed { index, entry -> "${index + 1}. <@${entry.key}>: ${entry.value}" }
                        .joinToString("\n").ifEmpty { "_Keine Punkte bisher vergeben_" })
                    append("\n\n")
                    for (l in 1..2) {
                        append("Liga $l:\n")
                        val league = db.league("IPLS4L$l")
                        for (i in 1..3) {
                            append("Platz $i: ")
                            append(leagues[l - 1].groupingBy { it.orderGuesses[i] }.eachCount()
                                .filterKeys { it != null }.entries.sortedByDescending { it.value }
                                .joinToString { "**${it.value}x** `${names[league.table[it.key!!]]}`" })
                            append("\n")
                        }
                        append("TopKiller-Guesses: ")
                        append(leagues[l - 1].groupingBy { it.topkiller }.eachCount()
                            .filterKeys { it != null }.entries.sortedByDescending { it.value }
                            .joinToString { "**${it.value}x** ${it.key}" })
                        append("\n\n")
                    }
                }, ephemeral = true
            )
        }

        object TipGameStats : CommandFeature<NoArgs>(
            NoArgs(),
            CommandSpec("tipgamestats", "Zeigt den aktuellen Stand des Tippspiels an.")
        ) {
            context(InteractionData)
            override suspend fun exec(e: NoArgs) {
                executeTipGameState(false)
            }
        }

        object TipGameStatsWOAdmin : CommandFeature<NoArgs>(
            NoArgs(),
            CommandSpec("tipgamestatswithoutorga", "Zeigt den aktuellen Stand des Tippspiels ohne Orga an.")
        ) {
            context(InteractionData)
            override suspend fun exec(e: NoArgs) {
                executeTipGameState(true)
            }
        }


        context(InteractionData) override suspend fun exec(e: NoArgs) {
            // do nothing
        }
    }

    object MostKillsButton : ButtonFeature<MostKillsButton.Args>(::Args, ButtonSpec("topkillerguess")) {
        class Args : Arguments() {
            var league by string()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            replyModal(MostKillsModal {
                this.league = e.league
            })
        }
    }

    object MostKillsModal : ModalFeature<MostKillsModal.Args>(::Args, ModalSpec("topkillerguess")) {
        override val title = "Top-Killer-Guess"

        class Args : Arguments() {
            var league by string().compIdOnly()
            var mon by draftPokemon("Pokemon", "Das Pokemon", {
                modal {
                    placeholder = "Das Pokemon, bitte so schreiben wie im Dokument!"
                }
            })
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            deferReply(true)
            val mon = e.mon
            if (!db.league(e.league).picks.values.flatten().any { it.name == mon.official }) {
                return reply("Dieses Pokemon wurde nicht in der Liga gepickt!", ephemeral = true)
            }
            val tlName = mon.tlName
            TipGameUserData.setTopKiller(user, e.league, tlName)
            reply("Dein persönlicher Top-Killer-Guess ist nun **$tlName**!", ephemeral = true)
        }
    }
}
