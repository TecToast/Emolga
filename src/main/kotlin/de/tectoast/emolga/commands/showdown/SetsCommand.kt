package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.emolga.utils.showdown.Analysis
import de.tectoast.emolga.utils.showdown.Player
import de.tectoast.emolga.utils.showdown.Pokemon
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.*
import java.util.regex.Pattern
import java.util.stream.Collectors

class SetsCommand : Command("sets", "Zeigt die Sets von einem Showdown-Kampf an", CommandCategory.Showdown) {
    private val client = OkHttpClient().newBuilder().build()

    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("url", "Replay-Link", "Der Replay-Link", ArgumentManagerTemplate.Text.any())
            .setExample("!sets https://replay.pokemonshowdown.com/oumonotype-82345404").build()
        beta()
    }

    @Throws(Exception::class)
    override fun process(e: GuildCommandEvent) {
        val url = e.arguments!!.getText("url")
        e.reply(Arrays.stream(Analysis(url, e.message).analyse()).map { p: Player ->
            try {
                val paste = buildPaste(p)
                //e.reply("```" + paste + "```");
                val res = client.newCall(
                    okhttp3.Request.Builder()
                        .url("https://pokepast.es/create")
                        .method(
                            "POST", okhttp3.FormBody.Builder()
                                .add("paste", paste)
                                .add("title", "Sets von " + p.nickname)
                                .add("author", "Emolga")
                                .add("notes", url)
                                .build()
                        )
                        .addHeader("Content-Type", "application/x-www-form-urlencoded")
                        .build()
                ).execute()
                val returl = res.request.url.toString()
                res.close()
                return@map p.nickname + ": " + returl
            } catch (ex: IOException) {
                ex.printStackTrace()
            }
            ""
        }.collect(Collectors.joining("\n")))
    }

    companion object {
        private val BEGIN_RETURN = Pattern.compile("^\\r\\n")
        private val END_RETURN = Pattern.compile("\\r\\n$")
        private fun buildPaste(p: Player): String {
            return p.mons.stream().map { pokemon: Pokemon -> buildPokemon(pokemon) }
                .map { string: String ->
                    END_RETURN.matcher(BEGIN_RETURN.matcher(string).replaceAll("")).replaceAll("")
                }
                .collect(Collectors.joining("\r\n\r\n"))
        }

        private fun buildPokemon(p: Pokemon): String {
            return buildString {
                append(if (p.nickname == p.pokemon) p.pokemon else p.nickname + " (${p.pokemon})")
                append(p.buildGenderStr())
                append(p.item?.let { s: String -> " @ $s" } ?: "")
                append("  \nAbility: ")
                append(p.ability.ifEmpty { "unknown" })
                append("  \n")
                append(p.moves.stream().map { s: String -> "- $s  " }.collect(Collectors.joining("\r\n")))
            }
        }
    }
}