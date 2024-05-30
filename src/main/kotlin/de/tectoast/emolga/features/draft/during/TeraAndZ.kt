package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.database.exposed.DraftName
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.TeraAndZ

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
            val config = league.getConfig<TeraAndZ>() ?: return reply(
                "Dieser Command ist hier nicht verfügbar!",
                ephemeral = true
            )
            replyModal(
                Modal(
                    title = "Bitte gib folgende Daten ein, falls du diese ändern möchtest",
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
                modal(modalKey = Tera)
            }.nullable()
            var type by pokemontype("type", "Dein Tera-Typ", english = true) {
                modal(modalKey = Tera)
            }.nullable()
            var z by string<DraftName>("z", "Dein Z-User") {
                validateDraftPokemon()
                modal(modalKey = Z)
            }.nullable()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val league = db.leagueByCommand() ?: return reply("Dieser Command ist hier nicht verfügbar!")
            val picks = league.picks[user]!!
            val config = league.getConfigOrDefault<TeraAndZ>()
            val b = league.builder()
            val index = league.index(user)
            val str = StringBuilder()
            config.z?.let { zconf ->
                e.z?.let {
                    if (!picks.any { p -> p.name == it.official }) return reply("Das Pokemon `${it.tlName}` befindet sich nicht in deinem Kader!")
                    b.addSingle(
                        zconf.coord(index),
                        "=WENNFEHLER(SVERWEIS(\"${it.tlName}\";${zconf.searchRange};${zconf.searchColumn};0))"
                    )
                    str.append("Z-User: `${it.tlName}`\n")
                }
            }
            config.tera?.let { tconf ->
                e.tera?.let {
                    if (!picks.any { p -> p.name == it.official }) return reply("Das Pokemon `${it.tlName}` befindet sich nicht in deinem Kader!")
                    val mon = tconf.mon
                    b.addSingle(
                        mon.coord(index),
                        "=WENNFEHLER(SVERWEIS(\"${it.tlName}\";${mon.searchRange};${mon.searchColumn};0))"
                    )
                    str.append("Tera-User: `${it.tlName}`\n")
                }
                e.type?.let {
                    val type = tconf.type
                    b.addSingle(
                        type.coord(index),
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

