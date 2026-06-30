package de.tectoast.emolga.features.league.laddertournament

import de.tectoast.emolga.domain.guildspecific.laddertournament.repository.LadderTournamentRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ButtonSpec
import de.tectoast.emolga.features.system.NoArgs
import de.tectoast.emolga.features.system.types.ButtonFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.generic.K18n_AlreadySignedUp
import de.tectoast.generic.K18n_SignupVerb
import org.koin.core.annotation.Single

@Single(binds = [ListenerProvider::class])
class LadderTournamentSignupButton(
    private val repo: LadderTournamentRepository,
    private val modal: LadderTournamentModal
) :
    ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("laddertournamentsignup")) {
    override val label = K18n_SignupVerb

    context(iData: InteractionData)
    override suspend fun exec(e: NoArgs) {
        iData.ephemeralDefault()
        if (repo.isSignedUp(iData.gid, iData.user)) return iData.reply(K18n_AlreadySignedUp)
        iData.replyModal(modal())
    }
}