package de.tectoast.emolga.features.various

import de.tectoast.emolga.database.exposed.NicknameCooldownsDB
import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.TimeUtils
import dev.minn.jda.ktx.coroutines.await
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

object NicknameCommand : CommandFeature<NicknameCommand.Args>(
    ::Args, CommandSpec("nickname", K18n_Nickname.Help)
) {
    class Args : Arguments() {
        var nickname by string("Nickname", K18n_Nickname.ArgNickname)
    }

    const val MAX_NICKNAME_LENGTH = 32

    @OptIn(ExperimentalTime::class)
    context(iData: InteractionData) override suspend fun exec(e: Args) {
        val member = iData.member()
        val nickname = e.nickname
        val g = iData.guild()
        if (!g.selfMember.canInteract(member)) {
            return iData.reply(K18n_Nickname.NotInteractable)
        }
        if (nickname.length > MAX_NICKNAME_LENGTH) {
            return iData.reply(K18n_Nickname.NicknameTooLong(MAX_NICKNAME_LENGTH))
        }
        val gid = g.idLong
        val uid = member.idLong
        NicknameCooldownsDB.getCooldown(gid, uid)?.let {
            return iData.reply(
                K18n_Nickname.TooSoon(
                    uid,
                    TimeUtils.secondsToTimePretty((it - Clock.System.now()).inWholeSeconds, iData.language)
                )
            )
        }
        member.modifyNickname(nickname).await()
        iData.reply(K18n_Nickname.Success(uid, nickname))
        NicknameCooldownsDB.setCooldown(gid, uid, Clock.System.now() + 7.days)
    }
}
