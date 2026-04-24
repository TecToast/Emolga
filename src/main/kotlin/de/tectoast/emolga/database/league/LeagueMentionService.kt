package de.tectoast.emolga.database.league

class LeagueMentionService(val leagueMemberRepo: LeagueMemberRepository) {
    suspend fun getMentionForParticipant(leagueName: String, idx: Int): MessageMentionData {
        val participants = leagueMemberRepo.getParticipantsForIdx(leagueName, idx)
        val (allSubstitute, main) = participants.partition { it.substitute }
        val pingSubstitutes = allSubstitute.filter { it.shouldPing }
        val mainContent = main.joinToString { "<@${it.userId}>" }
        val substituteContent =
            if (pingSubstitutes.isNotEmpty()) " ||(${pingSubstitutes.joinToString { "<@$it>" }})||" else ""
        val enabledMentions = participants.filter { it.shouldPing }.map { it.userId }
        return MessageMentionData(mainContent + substituteContent, enabledMentions)
    }
}

data class MessageMentionData(val content: String, val enabledMentions: List<Long>)