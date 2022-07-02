package de.tectoast.emolga.commands.showdown

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.CommandCategory
import de.tectoast.emolga.commands.GuildCommandEvent
import de.tectoast.jsolf.JSONArray
import de.tectoast.jsolf.JSONTokener
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URL

class SearchReplaysCommand :
    Command("searchreplays", "Sucht nach Replays der angegebenen Showdownbenutzernamen", CommandCategory.Showdown) {
    init {
        argumentTemplate = ArgumentManagerTemplate.builder()
            .add("user1", "User 1", "Der Showdown-Username von jemandem", ArgumentManagerTemplate.Text.any())
            .add(
                "user2",
                "User 2",
                "Der Showdown-Username eines potenziellen zweiten Users",
                ArgumentManagerTemplate.Text.any(),
                true
            )
            .setExample("!searchreplays TecToast")
            .build()
    }

    @Throws(IOException::class)
    override fun process(e: GuildCommandEvent) {
        var url: String? = "https://replay.pokemonshowdown.com/search.json?user="
        val args = e.arguments!!
        val user1 = args.getText("user1")
        url += if (args.has("user2")) toSDName(user1) + "&user2=" + toSDName(
            args.getText("user2")
        ) else toSDName(user1)
        logger.info(url)
        val array = JSONArray(JSONTokener(URL(url).openStream()))
        logger.info(array.toString(4))
        val str = StringBuilder()
        if (array.length() == 0) {
            if (args.has("user2")) e.reply("Es wurde kein Kampf zwischen " + user1 + " und " + args.getText("user2") + " hochgeladen!") else e.reply(
                "Es wurde kein Kampf von $user1 hochgeladen!"
            )
            return
        }
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            str.append(o.getString("p1")).append(" vs ").append(o.getString("p2"))
                .append(": https://replay.pokemonshowdown.com/").append(o.getString("id")).append("\n")
        }
        logger.info(str.toString())
        e.textChannel.sendMessage(str.toString()).queue()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SearchReplaysCommand::class.java)
    }
}