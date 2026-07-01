package de.tectoast.emolga.features.flo.memberselect

import de.tectoast.emolga.domain.league.member.repository.LeagueMemberRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.SelectMenuSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.UserSelectMenuFeature
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class MemberSelectSelectMenu(private val leagueMemberRepo: LeagueMemberRepository) :
    UserSelectMenuFeature<MemberSelectSelectMenu.Args>(
        ::Args, SelectMenuSpec("memberselect")
    ) {

    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var league by string().compIdOnly()
        var isForTeammate by boolean().compIdOnly().default(false)
        var members by multiOptionLong(1..25)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val league = e.league
        val isForTeammate = e.isForTeammate
        val memberIds = e.members
        if (isForTeammate) {
            leagueMemberRepo.setTeammates(league, memberIds)
        } else {
            leagueMemberRepo.setPrimaryUsers(league, memberIds)
        }
        iData.replyRaw(
            "Updated Table $isForTeammate for $league: " + memberIds.joinToString { mem -> "<@$mem>" },
            ephemeral = true
        )
    }
}