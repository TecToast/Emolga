package de.tectoast.emolga.features.flegmon.sleepkick

import de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.model.SleepKickInitiationResult
import de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.service.SleepKickService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.into
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class SleepKickCommand(private val btn: SleepKickButton, private val service: SleepKickService) :
    CommandFeature<SleepKickCommand.Args>(
        ::Args, CommandSpec(
            "sleepkick",
            "Startet einen Sleep-Kick-Vorgang".k18n
        )
    ) {
    class Args : Arguments() {
        val target by member("Target", "CALLING AN AIRSTRIKE".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val target = e.target
        when (val result = service.initiateVote(
            iData.gid,
            iData.user,
            iData.data.voiceChannel,
            target.idLong,
            target.voiceState?.channel?.idLong
        )) {
            SleepKickInitiationResult.InitiatorNotInVoiceChannel -> iData.replyRaw(
                "Du musst in einem Sprachkanal sein, um einen Sleep-Kick durchzuführen.",
                ephemeral = true
            )

            SleepKickInitiationResult.TargetNotInSameVoiceChannel -> iData.replyRaw(
                "Ihr müsst im selben Sprachkanal sein, um einen Sleep-Kick durchzuführen.",
                ephemeral = true
            )

            is SleepKickInitiationResult.Success -> {
                iData.replyRaw("Abstimmung für Sleep-Kick gegen ${target.effectiveName} gestartet!", components = btn {
                    this.id = result.voteId
                }.into())
            }
        }
    }
}