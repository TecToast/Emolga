package de.tectoast.emolga.features.flo.memberselect

import de.tectoast.emolga.features.K18n_ONLYDEVELOPER
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.k18n
import dev.minn.jda.ktx.messages.into
import org.koin.core.annotation.Single


@Single(binds = [ListenerProvider::class])
class MemberSelectCommand(private val menu: MemberSelectSelectMenu) : CommandFeature<MemberSelectCommand.Args>(
    ::Args, CommandSpec("memberselect", K18n_ONLYDEVELOPER)
) {
    init {
        restrict(flo)
    }

    class Args : Arguments() {
        var league by string("League", "League".k18n)
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.replyRaw(
            components = menu(valueRange = 1..25) {
                this.league = e.league
            }.into(), ephemeral = true
        )
    }
}
