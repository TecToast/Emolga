package de.tectoast.emolga.features.league.laddertournament

import de.tectoast.emolga.discord.ChannelInterface
import de.tectoast.emolga.domain.guildspecific.laddertournament.repository.LadderTournamentRepository
import de.tectoast.emolga.domain.guildspecific.laddertournament.service.LadderTournamentService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.league.K18n_LadderTournament
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.ModalSpec
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.features.system.types.ModalFeature
import de.tectoast.emolga.utils.msg
import de.tectoast.generic.K18n_SignupNoun
import dev.minn.jda.ktx.interactions.components.SelectOption
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single(binds = [ListenerProvider::class])
class LadderTournamentModal(
    private val service: LadderTournamentService,
    private val channelInterface: ChannelInterface,
    private val approveBtn: LadderTournamentApproveButton
) :
    ModalFeature<LadderTournamentModal.Args>(::Args, ModalSpec("laddertournamentmodal")) {
    override val title = K18n_SignupNoun

    class Args : Arguments() {

        private val repo: LadderTournamentRepository by inject()

        val sdName by string("SD-Name", K18n_LadderTournament.ModalArgSdName)
        val formats by fromListModal(
            "Formate",
            K18n_LadderTournament.ModalArgFormats,
            valueRange = null,
            optionsProvider = {
                val lt = repo.getConfigByGuild(it.gid) ?: return@fromListModal listOf(
                    SelectOption(
                        "No options",
                        "nooptions"
                    )
                )
                lt.formats.keys.map { f -> SelectOption(f, f) }
            })
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val result = service.handleSignupRequest(iData.gid, iData.user, e.sdName, e.formats, approveBtn)
        iData.reply(result.msg(), ephemeral = true)
    }
}