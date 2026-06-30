package de.tectoast.emolga.features.various

import de.tectoast.emolga.domain.moderation.nickname.service.NicknameService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.msg
import org.koin.core.annotation.Single
import kotlin.time.ExperimentalTime

@Single(binds = [ListenerProvider::class])
class NicknameCommand(
    private val service: NicknameService
) :
    CommandFeature<NicknameCommand.Args>(
        ::Args, CommandSpec("nickname", K18n_Nickname.Help)
    ) {
    class Args : Arguments() {
        var nickname by string("Nickname", K18n_Nickname.ArgNickname)
    }

    companion object {
        const val MAX_NICKNAME_LENGTH = 32
    }

    @OptIn(ExperimentalTime::class)
    context(iData: InteractionData) override suspend fun exec(e: Args) {
        iData.reply(service.changeNicknameRequest(iData.gid, iData.user, e.nickname).msg())
    }
}

