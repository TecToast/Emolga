package de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.model

sealed interface SleepKickInitiationResult {
    data object InitiatorNotInVoiceChannel : SleepKickInitiationResult
    data object TargetNotInSameVoiceChannel : SleepKickInitiationResult
    data class Success(val voteId: Long) : SleepKickInitiationResult
}