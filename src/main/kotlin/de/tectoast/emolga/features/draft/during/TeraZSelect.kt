package de.tectoast.emolga.features.draft.during

import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.IdxByUserIdResult
import de.tectoast.emolga.league.League
import dev.minn.jda.ktx.interactions.components.SelectOption
import dev.minn.jda.ktx.messages.editMessage
import dev.minn.jda.ktx.messages.into

object TeraZSelect {
    object Begin : ButtonFeature<Begin.Args>(::Args, ButtonSpec("teraselect")) {

        class Args : Arguments() {
            var league by string()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            ephemeralDefault()
            League.executeOnFreshLock(e.league) {
                val idx = when (val res = getIdxByUserId(user)) {
                    is IdxByUserIdResult.NotFound -> return@executeOnFreshLock reply("Du nimmst nicht an dieser Liga teil!")
                    is IdxByUserIdResult.Ambiguous -> return@executeOnFreshLock reply("Es ist uneindeutig, für wen du pickst!")
                    is IdxByUserIdResult.Success -> res.idx
                }
                val config = config.teraSelect ?: return@executeOnFreshLock reply(
                    "Dieser Command ist hier nicht verfügbar!"
                )
                val options = picks[idx]!!.filter { it.tier in config.tiers }.sortedWith(tierorderingComparator)
                    .map { SelectOption("${it.tier}: ${NameConventionsDB.convertOfficialToTL(it.name, gid)}", it.name) }
                reply("Bitte wähle deinen ${config.type}-User aus!", components = Menu(options = options) {
                    this.league = e.league
                    this.idx = idx
                }.into(), ephemeral = true)
            }
        }
    }

    object Menu : SelectMenuFeature<Menu.Args>(::Args, SelectMenuSpec("teraselect")) {
        class Args : Arguments() {
            var league by string().compIdOnly()
            var idx by int().compIdOnly()
            var mon by singleOption()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            League.executeOnFreshLock(e.league) {
                val mon = e.mon
                val idx = e.idx
                val data = persistentData.teraSelect
                data.selected[idx] = mutableSetOf(mon)
                save()
                reply("✅", ephemeral = true)
                data.mid?.let { tc.editMessage(it, content = generateCompletedText(data.selected.keys)).queue() }
            }
        }
    }
}