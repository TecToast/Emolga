package de.tectoast.emolga.features.wrc

import de.tectoast.emolga.database.exposed.WRCDataDB
import de.tectoast.emolga.database.exposed.WRCMatchupsDB
import de.tectoast.emolga.database.exposed.WRCMonsPickedDB
import de.tectoast.emolga.database.exposed.WRCTeraDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.add
import de.tectoast.emolga.utils.universalLogger
import dev.minn.jda.ktx.messages.edit
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji

object WRCMonSelect : SelectMenuFeature<WRCMonSelect.Args>(::Args, SelectMenuSpec("wrcmonselect")) {
    class Args : Arguments() {
        var wrcname by string().compIdOnly()
        var gameday by int().compIdOnly()
        var tier by string().compIdOnly()
        var selection by multiOption(2..2)
    }

    val teraTiers = setOf("A", "B")

    context(iData: InteractionData) override suspend fun exec(e: Args) {
        iData.deferEdit()
        if (WRCMatchupsDB.hasSubmittedTeam(
                e.wrcname, e.gameday, iData.user
            )
        ) return iData.reply("Du hast dein Team bereits abgegeben!")
        val selectedMons = e.selection
        WRCMonsPickedDB.setPickedMonsForTier(e.wrcname, e.gameday, iData.user, e.tier, selectedMons)
        val mons = WRCMonsPickedDB.getOrderedPickedMons(e.wrcname, e.gameday, iData.user)
        iData.message.edit(
            "## Deine Mons\n" + WRCMonsPickedDB.buildPickedMonsMessage(
                e.wrcname, e.gameday, iData.user, monsProvided = mons
            )
        ).queue()
        val (mid, tera) = WRCTeraDB.getDataForUser(e.wrcname, e.gameday, iData.user)
            ?: return universalLogger.warn("No Tera data found for {} {} {}", e.wrcname, e.gameday, iData.user)
        iData.messageChannel.editMessage(
            mid.toString(),
            components = WRCManager.buildTeraAndSubmitComponents(
                e.wrcname,
                e.gameday,
                mons.filter { it.tier in teraTiers }.map { it.name },
                tera
            )
        ).queue()
    }
}

object WRCMonSubmitButton : ButtonFeature<WRCMonSubmitButton.Args>(::Args, ButtonSpec("wrcmonsubmit")) {

    override val buttonStyle = ButtonStyle.SUCCESS
    override val label = "Team draften"
    override val emoji = Emoji.fromUnicode("✅")

    class Args : Arguments() {
        var wrcname by string().compIdOnly()
        var gameday by int().compIdOnly()
    }

    context(iData: InteractionData) override suspend fun exec(e: Args) {
        val tl = WRCDataDB.getTierlistOfWrcName(e.wrcname) ?: return
        val prices = tl.prices.toMutableMap()
        WRCMonsPickedDB.getUnorderedPickedMons(e.wrcname, e.gameday, iData.user).forEach { dp ->
            prices.add(dp.tier, -1)
        }
        if (prices.any { it.value != 0 }) {
            return iData.reply("Dein Team ist noch nicht vollständig!")
        }
        iData.message.edit(components = WRCMonSubmitButton(disabled = true) {
            this.wrcname = e.wrcname
            this.gameday = e.gameday
        }.into()).queue()
        iData.reply("Du hast dein Team für Spieltag ${e.gameday} bei ${e.wrcname} erfolgreich abgegeben!")
        WRCMatchupsDB.markSubmitted(e.wrcname, e.gameday, iData.user)
        WRCManager.checkIfAllSubmitted(e.wrcname, e.gameday, iData.user)
    }
}

object WRCTeraSelectMenu : SelectMenuFeature<WRCTeraSelectMenu.Args>(::Args, SelectMenuSpec("wrcteraselect")) {
    class Args : Arguments() {
        var wrcname by string().compIdOnly()
        var gameday by int().compIdOnly()
        var selection by singleOption()
    }

    context(iData: InteractionData) override suspend fun exec(e: Args) {
        if (WRCMatchupsDB.hasSubmittedTeam(
                e.wrcname, e.gameday, iData.user
            )
        ) return iData.reply("Du hast dein Team bereits abgegeben!")
        val selectedMon = e.selection
        if (selectedMon.isBlank()) return iData.reply("Da ist etwas nicht ganz richtig gelaufen :^)")
        WRCTeraDB.setTeraForUser(e.wrcname, e.gameday, iData.user, selectedMon)
        iData.message.edit(WRCManager.TERA_AND_SUBMIT_BASE_MESSAGE + "\n\nGewählter Tera-Captain: $selectedMon").queue()
    }
}
