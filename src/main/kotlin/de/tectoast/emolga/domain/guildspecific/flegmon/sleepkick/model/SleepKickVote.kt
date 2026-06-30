package de.tectoast.emolga.domain.guildspecific.flegmon.sleepkick.model

data class SleepKickVote(
    val target: Long,
    val allVoters: Set<Long>,
    val yesVoters: MutableSet<Long>
)