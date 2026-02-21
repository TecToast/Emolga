package de.tectoast.emolga.features.league

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.json.LadderTournamentUserData
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.generic.K18n_AlreadySignedUp
import de.tectoast.generic.K18n_Approve
import de.tectoast.generic.K18n_SignupNoun
import de.tectoast.generic.K18n_SignupVerb
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send

object LadderTournament {
    object SignupButton : ButtonFeature<NoArgs>(NoArgs(), ButtonSpec("laddertournamentsignup")) {
        override val label = K18n_SignupVerb

        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            iData.ephemeralDefault()
            val lt = mdb.ladderTournament.get(iData.gid)
                ?: return iData.reply(K18n_LadderTournament.NoTournamentHere)
            if (lt.users[iData.user]?.verified == true) return iData.reply(K18n_AlreadySignedUp)
            iData.replyModal(Modal())
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("laddertournamentmodal")) {
        override val title = K18n_SignupNoun

        class Args : Arguments() {
            val sdname by string("SD-Name", K18n_LadderTournament.ModalArgSdName)
            val formats by fromListModal(
                "Formate",
                K18n_LadderTournament.ModalArgFormats,
                valueRange = null,
                optionsProvider = {
                    val lt = mdb.ladderTournament.get(it.gid) ?: return@fromListModal listOf(
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
            val lt = mdb.ladderTournament.get(iData.gid) ?: return
            val uid = iData.user
            if (lt.users[uid]?.verified == true) return iData.reply("Du bist bereits angemeldet!", ephemeral = true)
            if (!e.sdname.startsWith(lt.sdNamePrefix)) return iData.reply(
                "Dein Showdown-Name muss mit `${lt.sdNamePrefix}` beginnen!",
                ephemeral = true
            )
            lt.users[uid] = LadderTournamentUserData(e.sdname, e.formats, verified = false)
            lt.save()
            iData.reply("Deine Anmeldung ist angekommen und wird nun vom Komitee verifiziert!", ephemeral = true)
            iData.jda.getTextChannelById(lt.adminChannel)!!
                .send("Anmeldungsanfrage:\nUser: <@${uid}>\nSD-Name: `${e.sdname}`", components = ApproveButton {
                    this.user = uid
                }.into()).queue()
        }
    }

    object ApproveButton : ButtonFeature<ApproveButton.Args>(::Args, ButtonSpec("laddertournamentapprove")) {
        override val label = K18n_Approve

        class Args : Arguments() {
            var user by long()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val lt = mdb.ladderTournament.get(iData.gid)
                ?: return iData.reply("Auf diesem Server gibt es kein laufendes Ladder-Turnier!", ephemeral = true)
            val data = lt.users[e.user] ?: return iData.reply(
                "Dieser User hat keine Anmeldung zum Ladder-Turnier!",
                ephemeral = true
            )

            iData.edit(contentK18n = null, components = emptyList())
            data.verified = true
            lt.save()
            iData.reply(K18n_LadderTournament.Verified, ephemeral = true)
            iData.jda.getTextChannelById(lt.signupChannel)!!.send("<@${e.user}>: ${data.formats.joinToString()}")
                .queue()
        }
    }
}
