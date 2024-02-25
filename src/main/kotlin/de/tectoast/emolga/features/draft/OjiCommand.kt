package de.tectoast.emolga.features.draft

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.embedColor
import de.tectoast.emolga.utils.json.TipGameUserData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.League
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import net.dv8tion.jda.api.interactions.components.ActionRow
import org.litote.kmongo.eq

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
            val tlName = e.mon.tlName
            TipGameUserData.setTopKiller(user, e.league, tlName)
            reply("Dein persönlicher Top-Killer-Guess ist nun **$tlName**!", ephemeral = true)
        }
    }
}