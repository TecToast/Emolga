package de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.model


sealed interface SleepKickVoteResult {
    data object VotingNotFound : SleepKickVoteResult
    data object VoteAlreadyCast : SleepKickVoteResult
    data class VoteSuccessful(val yesVoters: Int, val requiredVotes: Int) : SleepKickVoteResult
    data object VoteFinished : SleepKickVoteResult
}