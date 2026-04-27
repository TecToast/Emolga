package de.tectoast.emolga.features.flegmon

import de.tectoast.emolga.features.Arguments
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.InteractionData
import de.tectoast.emolga.ktor.DSBMessage
import de.tectoast.emolga.ktor.dsbFlow
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.mdb
import de.tectoast.emolga.utils.k18n
import de.tectoast.emolga.utils.toSDName

object DSB {
    object WithMon : CommandFeature<WithMon.Args>(::Args, CommandSpec("dsbmon", "dsbmon".k18n)) {
        class Args : Arguments() {
            val mon by draftPokemon("mon", "mon".k18n)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            val otherOfficial = e.mon.otherOfficial!!
            dsbFlow.emit(
                DSBMessage(
                    iData.user.toString(),
                    e.mon.official + " / " + otherOfficial,
                    "/api/emolga/monimg/SUGIMORI/${
                        mdb.pokedex.get(otherOfficial.toSDName())!!.calcSpriteName()
                    }.png",
                    System.currentTimeMillis().toHexString()
                )
            )
            iData.done(true)
        }
    }

    object Text : CommandFeature<Text.Args>(::Args, CommandSpec("dsbtext", "dsbtext".k18n)) {
        class Args : Arguments() {
            val text by string("text", "text".k18n)
        }

        context(iData: InteractionData)
        override suspend fun exec(e: Args) {
            dsbFlow.emit(DSBMessage(iData.user.toString(), e.text, null, System.currentTimeMillis().toHexString()))
            iData.done(true)
        }
    }
}
