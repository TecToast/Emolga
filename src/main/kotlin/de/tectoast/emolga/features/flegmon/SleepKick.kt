package de.tectoast.emolga.features.flegmon

import de.tectoast.emolga.features.*
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.UserSnowflake

object SleepKick {
    private val activeVotings = mutableMapOf<Long, SleepKickVote>()

    object Command :
        CommandFeature<Command.Args>(::Args, CommandSpec("sleepkick", "Startet einen Sleep-Kick-Vorgang")) {
        class Args : Arguments() {
            val target by member("Target", "CALLING AN AIRSTRIKE")
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val target = e.target
            val vc = iData.member().voiceState?.channel
            val selfVoiceChannelId = vc?.idLong ?: return iData.reply(
                "Du musst in einem Sprachkanal sein, um einen Sleep-Kick durchzuführen.",
                ephemeral = true
            )
            if (selfVoiceChannelId != target.voiceState?.channel?.idLong) {
                return iData.reply(
                    "Ihr müsst im selben Sprachkanal sein, um einen Sleep-Kick durchzuführen.",
                    ephemeral = true
                )
            }
            val id = System.currentTimeMillis()
            activeVotings[id] = SleepKickVote(
                target = target.idLong,
                allVoters = vc.members.mapTo(mutableSetOf()) { it.idLong },
                yesVoters = mutableSetOf(iData.user)
            )
            iData.reply("Abstimmung für Sleep-Kick gegen ${target.effectiveName} gestartet!", components = Button {
                this.id = id
            }.into())
        }
    }

    object Button : ButtonFeature<Button.Args>(::Args, ButtonSpec("sleepkick_vote")) {
        override val buttonStyle = ButtonStyle.PRIMARY
        override val label = "Für Sleep-Kick stimmen"

        class Args : Arguments() {
            var id by long().compIdOnly()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val data = activeVotings[e.id] ?: return iData.reply(
                "Diese Abstimmung ist nicht mehr aktiv.",
                ephemeral = true
            )
            if (data.yesVoters.add(iData.user)) {
                val totalVoters = data.allVoters.size
                val yesVoters = data.yesVoters.size
                val requiredVotes = (totalVoters + 1) / 2
                if (yesVoters >= requiredVotes) {
                    val guild = iData.guild
                    guild().kickVoiceMember(UserSnowflake.fromId(data.target)).queue()
                    activeVotings.remove(e.id)
                    iData.reply("Die Abstimmung war erfolgreich!", ephemeral = true)
                    iData.message.delete().queue()
                } else {
                    iData.reply("Deine Stimme wurde gezählt! ($yesVoters/$requiredVotes Stimmen)", ephemeral = true)
                }
            } else {
                iData.reply("Du hast bereits für den Sleep-Kick abgestimmt.", ephemeral = true)
            }
        }
    }
}

private data class SleepKickVote(
    val target: Long,
    val allVoters: Set<Long>,
    val yesVoters: MutableSet<Long>
)