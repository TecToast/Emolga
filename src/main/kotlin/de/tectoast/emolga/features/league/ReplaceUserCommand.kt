package de.tectoast.emolga.features.league

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.json.SignUpInput
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.teamgraphics.TeamGraphicGenerator
import org.litote.kmongo.eq
import org.litote.kmongo.pos
import org.litote.kmongo.set
import org.litote.kmongo.setTo

object ReplaceUserCommand :
    CommandFeature<ReplaceUserCommand.Args>(::Args, CommandSpec("replaceuser", K18n_ReplaceUser.Help)) {
    class Args : Arguments() {
        val oldUser by member("OldUser", K18n_ReplaceUser.ArgOldUser)
        val newUser by member("NewUser", K18n_ReplaceUser.ArgNewUser)
        val sdName by string("SDName", K18n_ReplaceUser.ArgSDName).nullable()
        val teamName by string("TeamName", K18n_ReplaceUser.ArgTeamName).nullable()
    }

    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        val uidOld = e.oldUser.idLong
        val uidNew = e.newUser.idLong
        val league = mdb.leagueByGuild(iData.gid, uidOld) ?: return iData.reply(K18n_ReplaceUser.OldUserNotInLeague)
        val idx = league(uidOld)
        mdb.league.updateOne(
            League::leaguename eq league.leaguename,
            set(League::table.pos(idx) setTo uidNew)
        )
        league.getSignup()?.let { signup ->
            val uData = signup.users.first { it.users.contains(uidOld) }
            uData.users.apply {
                remove(uidOld)
                add(uidNew)
            }
            e.sdName?.let {
                uData.data[SignUpInput.SDNAME_ID] = it
            }
            e.teamName?.let {
                uData.data[SignUpInput.TEAMNAME_ID] = it
            } ?: TeamGraphicGenerator.editTeamGraphicForLeague(league, idx)
            signup.save()
        }
        iData.done(true)
    }
}