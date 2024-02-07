package de.tectoast.emolga.features.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.database.exposed.SpoilerTagsDB
import de.tectoast.emolga.features.CommandFeature
import de.tectoast.emolga.features.CommandSpec
import de.tectoast.emolga.features.NoArgs

object SpoilerTagsCommand : CommandFeature<NoArgs>(
    NoArgs(),
    CommandSpec(
        "spoilertags",
        "Aktiviert oder deaktiviert den Spoilerschutz bei Showdown-Ergebnissen. (Gilt serverweit)",
        -1
    )
) {
    context(InteractionData)
    override suspend fun exec(e: NoArgs) {
        if (SpoilerTagsDB.delete(gid)) {
            reply("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **deaktiviert**!")
            Command.spoilerTags.remove(gid)
            return
        }
        SpoilerTagsDB.insert(gid)
        Command.spoilerTags.add(gid)
        reply("Auf diesem Server sind Spoiler-Tags bei Showdown-Ergebnissen nun **aktiviert**!")
    }
}
