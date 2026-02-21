package de.tectoast.emolga.features.league.draft

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.*
import de.tectoast.emolga.features.league.draft.generic.K18n_NoLeagueForGuildFound
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.b
import de.tectoast.emolga.utils.invoke
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.t
import de.tectoast.generic.K18n_CommandNotAvailable

object TeraAndZ {

    object Tera : ModalKey
    object Z : ModalKey

    object Command : CommandFeature<NoArgs>(
        NoArgs(),
        CommandSpec("teraandz", K18n_TeraAndZ.Help)
    ) {
        context(iData: InteractionData)
        override suspend fun exec(e: NoArgs) {
            val league = mdb.leagueByCommand() ?: return iData.reply(
                K18n_NoLeagueForGuildFound,
                ephemeral = true
            )
            val config = league.config.teraAndZ ?: return iData.reply(
                K18n_CommandNotAvailable,
                ephemeral = true
            )
            iData.replyModal(
                Modal(
                    title = K18n_TeraAndZ.ModalTitle,
                    specificallyEnabledArgs = mapOf(
                        Z to (config.z != null),
                        Tera to (config.tera != null)
                    )
                )
            )
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("teraandz")) {
        class Args : Arguments() {
            var tera by string<DraftName>("tera", K18n_TeraAndZ.ArgTeraUser) {
                validateDraftPokemon()
                modal(modalKey = Tera)
            }.nullable()
            var type by pokemontype("type", K18n_TeraAndZ.ArgTeraType, Language.ENGLISH) {
                modal(modalKey = Tera)
            }.nullable()
            var z by string<DraftName>("z", K18n_TeraAndZ.ArgZUser) {
                validateDraftPokemon()
                modal(modalKey = Z)
            }.nullable()
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val league =
                mdb.leagueByCommand() ?: return iData.reply(K18n_CommandNotAvailable, ephemeral = true)
            val config =
                league.config.teraAndZ ?: return iData.reply(
                    K18n_CommandNotAvailable,
                    ephemeral = true
                )
            val idx = league(iData.user)
            val picks = league.picks[idx]!!
            val b = league.builder()
            val str = StringBuilder()
            config.z?.let { zconf ->
                e.z?.let {
                    val selected = picks.firstOrNull { p -> p.name == it.official } ?: return iData.reply(
                        K18n_TeraAndZ.PokemonNotInTeam(it.tlName),
                        ephemeral = true
                    )
                    zconf.firstTierAllowed?.let { tier ->
                        val order = league.tierlist.order

                        if (order.indexOf(selected.tier) < order.indexOf(tier)) return iData.reply(
                            b { "Z-User " + K18n_TeraAndZ.TierInvalid(tier, it.tlName, selected.tier)() },
                            ephemeral = true
                        )
                    }
                    b.addSingle(
                        zconf.coord(idx),
                        "=WENNFEHLER(SVERWEIS(\"${it.tlName}\";${zconf.searchRange};${zconf.searchColumn};0))"
                    )
                    str.append("Z-User: `${it.tlName}`\n")
                }
            }
            config.tera?.let { tconf ->
                e.tera?.let {
                    val selected = picks.firstOrNull { p -> p.name == it.official } ?: return iData.reply(
                        K18n_TeraAndZ.PokemonNotInTeam(it.tlName),
                        ephemeral = true
                    )
                    tconf.mon.firstTierAllowed?.let { tier ->
                        val order = league.tierlist.order
                        if (order.indexOf(selected.tier) < order.indexOf(tier)) return iData.reply(
                            b { "Tera-User " + K18n_TeraAndZ.TierInvalid(tier, it.tlName, selected.tier)() },
                            ephemeral = true
                        )
                    }
                    val mon = tconf.mon
                    b.addSingle(
                        mon.coord(idx),
                        "=WENNFEHLER(SVERWEIS(\"${it.tlName}\";${mon.searchRange};${mon.searchColumn};0))"
                    )
                    str.append("Tera-User: `${it.tlName}`\n")
                }
                e.type?.let {
                    val type = tconf.type
                    b.addSingle(
                        type.coord(idx),
                        "=WENNFEHLER(SVERWEIS(\"${it}\";${type.searchRange};${type.searchColumn};0))"
                    )
                    str.append("Tera-Type: `${it}`")
                }
            }
            b.execute()
            if (str.isEmpty()) str.append(K18n_TeraAndZ.NoChanges.t())
            iData.reply(K18n_TeraAndZ.ChangesText(iData.user, str.toString()))
        }
    }
}

