package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db

object TeraAndZ {

    object Tera : ModalKey
    object Z : ModalKey

    object Command : CommandFeature<NoArgs>(
        NoArgs(),
        CommandSpec("teraandz", "Stellt deinen Tera- und Z-User ein", Constants.G.NDS)
    ) {
        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            val league = db.leagueByCommand() ?: return reply(
                "Du nimmst nicht an einer Liga auf diesem Server teil!",
                ephemeral = true
            )
            val config = league.config.teraAndZ ?: return reply(
                "Dieser Command ist hier nicht verfügbar!",
                ephemeral = true
            )
            replyModal(
                Modal(
                    title = "Änderungswünsche für Tera- und Z-User",
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
            var tera by string<DraftName>("tera", "Dein Tera-User") {
                validateDraftPokemon()
                modal(modalKey = Tera) {
                    placeholder = "Tera-User oder sonst leer lassen"
                }
            }.nullable()
            var type by pokemontype("type", "Dein Tera-Typ", english = true) {
                modal(modalKey = Tera) {
                    placeholder = "Tera-Typ oder leer lassen"
                }
            }.nullable()
            var z by string<DraftName>("z", "Dein Z-User") {
                validateDraftPokemon()
                modal(modalKey = Z) {
                    placeholder = "Z-User oder leer lassen"
                }
            }.nullable()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val league =
                db.leagueByCommand() ?: return reply("Dieser Command ist hier nicht verfügbar!", ephemeral = true)
            val idx = league.index(user)
            val picks = league.picks[idx]!!
            val config =
                league.config.teraAndZ ?: return reply("Dieser Command ist hier nicht verfügbar!", ephemeral = true)
            val b = league.builder()
            val str = StringBuilder()
            config.z?.let { zconf ->
                e.z?.let {
                    val selected = picks.firstOrNull { p -> p.name == it.official } ?: return reply(
                        "Das Pokemon `${it.tlName}` befindet sich nicht in deinem Kader!",
                        ephemeral = true
                    )
                    zconf.firstTierAllowed?.let { tier ->
                        val order = league.tierlist.order
                        if (order.indexOf(selected.tier) < order.indexOf(tier)) return reply(
                            "Z-User dürfen maximal im ${tier}-Tier sein, `${it.tlName}` befindet sich im ${selected.tier}-Tier!",
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
                    val selected = picks.firstOrNull { p -> p.name == it.official } ?: return reply(
                        "Das Pokemon `${it.tlName}` befindet sich nicht in deinem Kader!",
                        ephemeral = true
                    )
                    tconf.mon.firstTierAllowed?.let { tier ->
                        val order = league.tierlist.order
                        if (order.indexOf(selected.tier) < order.indexOf(tier)) return reply(
                            "Tera-User dürfen maximal im ${tier}-Tier sein, `${it.tlName}` befindet sich im ${selected.tier}-Tier!",
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
                    str.append("Tera-Typ: `${it}`")
                }
            }
            b.execute()
            if (str.isEmpty()) str.append("_Keine Änderung vorgenommen_")
            reply("Änderungen von <@${user}>:\n" + str.toString())
        }
    }
}

