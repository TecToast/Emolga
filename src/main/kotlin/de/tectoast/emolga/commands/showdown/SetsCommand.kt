@file:Suppress("UNUSED_PARAMETER")

package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.showdown.SDPlayer
import de.tectoast.emolga.utils.showdown.SDPokemon
import java.util.regex.Pattern

@Suppress("unused")
class SetsCommand : Command("sets", "Zeigt die Sets von einem Showdown-Kampf an", CommandCategory.Showdown) {

    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("url", "Replay-Link", "Der Replay-Link", ArgumentManagerTemplate.Text.any())
            .setExample("!sets https://replay.pokemonshowdown.com/oumonotype-82345404").build()
        beta()
        disable()
    }

    @Throws(Exception::class)
    override suspend fun process(e: GuildCommandEvent) {
        /*val url = e.arguments.getText("url")
        e.reply(Analysis(url, e.message).analyse(e.channel).asFlow().map {
            val paste = buildPaste(it)
            val res = httpClient.post("https://pokepast.es/create") {
                setBody(FormDataContent(Parameters.build {
                    append("paste", paste)
                    append("title", "Sets von ${it.nickname}")
                    append("author", "Emolga")
                    append("notes", url)
                }))
            }
            it.nickname + ": " + res.request.url.toString()
        }.toList().joinToString("\n"))*/
    }

    companion object {
        private val BEGIN_RETURN = Pattern.compile("^\\r\\n")
        private val END_RETURN = Pattern.compile("\\r\\n$")
        private fun buildPaste(p: SDPlayer): String {
            return p.pokemon.map { buildPokemon(it) }.joinToString("\r\n\r\n") {
                END_RETURN.matcher(BEGIN_RETURN.matcher(it).replaceAll("")).replaceAll("")
            }

        }

        private fun buildPokemon(p: SDPokemon): String {
            return buildString {
                /*append(if (p.nickname == p.pokemon) p.pokemon else p.nickname + " (${p.pokemon})")
                append(p.buildGenderStr())
                append(p.item?.let { s: String -> " @ $s" } ?: "")
                append("  \nAbility: ")
                append(p.ability.ifEmpty { "unknown" })
                append("  \n")
                append(p.moves.joinToString("\r\n") { "- $it  " })*/
            }
        }
    }
}
