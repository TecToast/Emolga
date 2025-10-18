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
    ::Args, CommandSpec("nickname", "Ändert deinen Nickname (funktioniert nur 1x pro Woche)")
) {
    class Args : Arguments() {
        var nickname by string("Nickname", "Der neue Nickname")
    }

    @OptIn(ExperimentalTime::class)
    context(iData: InteractionData) override suspend fun exec(e: Args) {
        val member = iData.member()
        val nickname = e.nickname
        val g = iData.guild()
        if (!g.selfMember.canInteract(member)) {
            return iData.reply("Ich kann deinen Nickname nicht modifizieren!")
        }
        if (nickname.length > 32) {
            return iData.reply("Dieser Nickname ist zu lang! (Er darf maximal 32 Zeichen enthalten)")
        }
        val gid = g.idLong
        val uid = member.idLong
        NicknameCooldownsDB.getCooldown(gid, uid)?.let {
            return iData.reply(
                "${member.asMention} Du kannst deinen Namen noch nicht wieder ändern!\nCooldown: ${
                    TimeUtils.secondsToTimePretty((it - Clock.System.now()).inWholeSeconds)
                }"
            )
        }
        member.modifyNickname(nickname).await()
        iData.reply(member.asMention + " Dein Nickname wurde erfolgreich geändert!")
        NicknameCooldownsDB.setCooldown(gid, uid, Clock.System.now() + 7.days)
    }
}
