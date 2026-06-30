package de.tectoast.emolga.features.flegmon.sleepkick

import de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.model.SleepKickVoteResult
import de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.service.SleepKickService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SleepKickButton(private val service: SleepKickService) : ButtonFeature<SleepKickButton.Args>(
    ::Args,
    ButtonSpec("sleepkick_vote")
) {
    override val buttonStyle = ButtonStyle.PRIMARY
    override val label = "Für Sleep-Kick stimmen".k18n

    class Args : Arguments() {
        var id by long().compIdOnly()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyRaw(
            when (val result = service.handleVote(iData.gid, iData.tc, iData.data.messageId!!, e.id, iData.user)) {
                SleepKickVoteResult.VotingNotFound -> "Diese Abstimmung ist nicht mehr aktiv."
                SleepKickVoteResult.VoteAlreadyCast -> "Du hast bereits für den Sleep-Kick abgestimmt."
                is SleepKickVoteResult.VoteSuccessful -> "Deine Stimme wurde gezählt! (${result.yesVoters}/${result.requiredVotes} Stimmen)"
                SleepKickVoteResult.VoteFinished -> "Die Abstimmung war erfolgreich!"
            }, ephemeral = true
        )
    }
}