package de.tectoast.emolga.features.various

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.TimeUtils
import de.tectoast.emolga.utils.json.Cooldown
import de.tectoast.emolga.utils.json.db
import dev.minn.jda.ktx.coroutines.await
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.upsert

object NicknameCommand : CommandFeature<NicknameCommand.Args>(
    ::Args,
    CommandSpec("nickname", "Ändert deinen Nickname (funktioniert nur 1x pro Woche)", Constants.G.ASL)
) {
    class Args : Arguments() {
        var nickname by string("Nickname", "Der neue Nickname")
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
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
        db.cooldowns.findOne(Cooldown::guild eq gid, Cooldown::user eq uid)?.run {
            val expiresIn = this.timestamp - System.currentTimeMillis()
            if (expiresIn <= 0) return@run
            return iData.reply(
                "${member.asMention} Du kannst deinen Namen noch nicht wieder ändern!\nCooldown: ${
                    TimeUtils.secondsToTimePretty(expiresIn / 1000)
                }"
            )
        }
        member.modifyNickname(nickname).await()
        iData.reply(member.asMention + " Dein Nickname wurde erfolgreich geändert!")
        db.cooldowns.updateOne(
            and(Cooldown::guild eq gid, Cooldown::user eq uid),
            Cooldown(gid, uid, System.currentTimeMillis() + 604800000),
            upsert()
        )
    }
}
