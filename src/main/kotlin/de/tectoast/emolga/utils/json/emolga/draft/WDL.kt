package de.tectoast.emolga.utils.json.emolga.draft

import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.commands.coordXMod
import de.tectoast.emolga.utils.DraftTimer
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.TimerInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@SerialName("WDL")
class WDL(private val division: String) : League() {

    override val timerSkipMode = TimerSkipMode.AFTER_DRAFT
    override val teamsize = 12

    @Transient
    override val timer = DraftTimer(TimerInfo(6, 24), 60 * 4)
    override fun isPicked(mon: String, tier: String?) = if (tier == "PARADOX") false else super.isPicked(mon, tier)

    override fun RequestBuilder.pickDoc(data: PickData) {
        doc(data)
    }

    override fun RequestBuilder.switchDoc(data: SwitchData) {
        doc(data)
    }

    private fun RequestBuilder.doc(data: DraftData) {
        addSingle(
            data.memIndex.coordXMod(
                "$division-Kader", 6, 2, 2, 17,
                data.changedOnTeamsiteIndex + 9
            ), data.pokemon
        )
    }

    override fun manipulatePossibleTiers(possible: MutableMap<String, Int>) {
        if (!isLastRound) {
            possible["PARADOX"] = 0
        }
    }

    override fun handleTiers(
        e: GuildCommandEvent,
        specifiedTier: String,
        officialTier: String,
        fromSwitch: Boolean
    ): Boolean {
        if (round < 10 && specifiedTier == "PARADOX" && !fromSwitch) {
            e.reply("Du kannst erst in Runde 10 ein Paradox-Pokemon picken!")
            return true
        }
        return super.handleTiers(e, specifiedTier, officialTier, fromSwitch)
    }

    override fun checkUpdraft(specifiedTier: String, officialTier: String): String? {
        if (specifiedTier == "PARADOX" && officialTier != "PARADOX") return "Im Paradox-Tier darf nur ein Paradox-Pokemon gepickt werden!"
        return null
    }
}
