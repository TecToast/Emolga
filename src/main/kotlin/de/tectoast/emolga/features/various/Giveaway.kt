package de.tectoast.emolga.features.various


import de.tectoast.emolga.database.dbTransaction
import de.tectoast.emolga.database.exposed.Giveaway
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.TimeUtils
import de.tectoast.emolga.utils.embedColor
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import net.dv8tion.jda.api.entities.emoji.Emoji

object Giveaway {
    object GCreateCommand : CommandFeature<NoArgs>(
        NoArgs(), CommandSpec(
            "gcreate", "Erstellt ein Giveaway in diesem Channel",
            Constants.G.ASL,
            Constants.G.GENSHINEMPIRES,
            Constants.G.CULT,
            Constants.G.FPL,
            Constants.G.FLP,
            Constants.G.NDS
        )
    ) {
        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            replyModal(Modal())
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("gcreate")) {
        override val title = "Giveaway-Erstellung"

        class Args : Arguments() {
            var time by string<Long>("Dauer des Giveaways") {
                modal {
                    placeholder = "5h"
                    setRequiredRange(1, 20)
                }
                validate { s ->
                    TimeUtils.parseShortTime(s).takeIf { it > 0 }
                }
                customErrorMessage = "Das ist keine gültige Zeit!"
            }
            var winners by string<Int>("Anzahl der Gewinner") {
                modal {
                    placeholder = "1"
                    setRequiredRange(1, 2)
                }
                validate { s ->
                    s.toIntOrNull()?.takeIf { it > 0 }
                }
                customErrorMessage = "Du musst eine valide Anzahl an Gewinnern angegeben!"
            }
            var prize by string("Preis") {
                modal {
                    placeholder = "Kekse"
                    setRequiredRange(1, 100)
                }
            }
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            reply("Giveaway erstellt!", ephemeral = true)
            val end = Instant.fromEpochMilliseconds(System.currentTimeMillis() + e.time * 1000)
            val winners = e.winners
            val message = textChannel.sendMessageEmbeds(Embed {
                color = embedColor
                title = "Giveaway: ${e.prize}"
                footer("${if (winners == 1) "" else "$winners Gewinner | "}Endet")
                timestamp = end.toJavaInstant()
                description =
                    "Reagiere mit ${Constants.GIVEAWAY_EMOTE_MENTION} um teilzunehmen!\nGehostet von: <@$user>"
            }).await()
            dbTransaction {
                val g = Giveaway.new {
                    this.end = end
                    this.prize = prize
                    this.winners = winners
                    this.messageid = message.idLong
                    this.channelid = tc
                    this.hostid = user
                }
                message.addReaction(Emoji.fromCustom(Constants.GIVEAWAY_EMOTE_NAME, Constants.GIVEAWAY_EMOTE_ID, false))
                    .queue()
                g.startTimer()
            }
        }
    }
}
