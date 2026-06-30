package de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.service

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.discord.VoiceStateRepository
import de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.model.SleepKickInitiationResult
import de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.model.SleepKickVote
import de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.model.SleepKickVoteResult
import org.koin.core.annotation.Single
import kotlin.time.Clock

@Single
class SleepKickService(
    private val clock: Clock,
    private val voiceStateRepo: VoiceStateRepository,
    private val channelInterface: ChannelInterface
) {
    private val activeVotings = mutableMapOf<Long, SleepKickVote>()
    suspend fun initiateVote(
        guild: Long,
        initiator: Long,
        initiatorVoiceChannel: Long?,
        targetId: Long,
        targetVoiceChannel: Long?
    ): SleepKickInitiationResult {
        val selfVoiceChannelId = initiatorVoiceChannel ?: return SleepKickInitiationResult.InitiatorNotInVoiceChannel
        if (selfVoiceChannelId != targetVoiceChannel) {
            return SleepKickInitiationResult.TargetNotInSameVoiceChannel
        }
        val id = clock.now().toEpochMilliseconds()
        activeVotings[id] = SleepKickVote(
            target = targetId,
            allVoters = voiceStateRepo.getMembersInVoiceChannel(guild, selfVoiceChannelId),
            yesVoters = mutableSetOf(initiator)
        )
        return SleepKickInitiationResult.Success(id)
    }

    suspend fun handleVote(guild: Long, channel: Long, message: Long, id: Long, user: Long): SleepKickVoteResult {
        val data = activeVotings[id] ?: return SleepKickVoteResult.VotingNotFound
        if (data.yesVoters.add(user)) {
            val totalVoters = data.allVoters.size
            val yesVoters = data.yesVoters.size
            val requiredVotes = (totalVoters + 1) / 2
            if (yesVoters >= requiredVotes) {
                voiceStateRepo.kickVoiceMember(guild, data.target)
                activeVotings.remove(id)
                channelInterface.deleteMessage(channel, message)
                return SleepKickVoteResult.VoteFinished
            } else {
                return SleepKickVoteResult.VoteSuccessful(yesVoters, requiredVotes)
            }
        } else {
            return SleepKickVoteResult.VoteAlreadyCast
        }
    }
}