package de.tectoast.emolga.domain.game.service.process

import de.tectoast.emolga.domain.config.model.GuildConfigType
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.game.model.KDWithName
import de.tectoast.emolga.domain.game.model.ResultMessage
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
import de.tectoast.emolga.domain.pokemon.model.ShowdownID
import de.tectoast.emolga.domain.pokemon.service.PokemonDisplayService
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.Language
import de.tectoast.emolga.utils.showdown.K18n_Analysis
import de.tectoast.k18n.generated.K18nLanguage
import mu.KotlinLogging
import org.koin.core.annotation.Single

@Single
class ResultMessageBuilder(
    private val displayService: PokemonDisplayService,
    private val configRepo: GuildConfigRepository,
    private val botConstants: BotConstants,
    private val tierlistRepo: TierlistRepository
) {
    private val logger = KotlinLogging.logger {}
    suspend fun getResultMessages(
        game: List<List<KDWithName>>,
        is4v4: Boolean,
        language: K18nLanguage,
        dontTranslateFromReplayServer: Boolean,
        playerNames: List<String>,
        gid: Long,
        defaultNameLookup: Map<ShowdownID, String> = emptyMap(),
        urlIfPresent: String? = null,
    ): List<ResultMessage> {
        val isEnglishResults = language == K18nLanguage.EN || configRepo.query(
            gid,
            GuildConfigType.EnglishResults
        ) || tierlistRepo.getAllMetasForGuild(gid)
            .any { it.language == Language.ENGLISH }
        val pokemonLang =
            if (dontTranslateFromReplayServer) null else if (isEnglishResults) Language.ENGLISH else Language.GERMAN
        val spoiler = configRepo.query(gid, GuildConfigType.SpoilerTags)
        val description = generateDescription(
            game = game,
            spoiler = spoiler,
            is4v4 = is4v4,
            kLang = language,
            pokemonLang = pokemonLang,
            guildId = gid,
            playerNames = playerNames,
            defaultNameLookup = defaultNameLookup
        )
        val resultMessages = mutableListOf<ResultMessage>()
        resultMessages += ResultMessage.Game(description)
        var illusionMonPresent = false
        for ((index, ga) in game.withIndex()) {
            if (ga.containsIllusionMon()) {
                resultMessages += ResultMessage.IllusionWarning(playerNames[index])
                illusionMonPresent = true
            }
        }
        if (gid != botConstants.botOwnerGuildId && game.totalKDCount().let { it.first != it.second }) {
            resultMessages += ResultMessage.KillsDeathsNotMatching(illusionMonPresent)
            logger.warn((if (illusionMonPresent) "Zoroark... " else "") + "Kills don't match Deaths $urlIfPresent $game\n\n${description}")
        }
        return resultMessages
    }

    private fun Iterable<KDWithName>.containsIllusionMon() =
        any { it.name.value.startsWith("zoroark") || it.name.value.startsWith("zorua") }

    private fun Iterable<Iterable<KDWithName>>.totalKDCount() = fold(0 to 0) { old, game ->
        val (kills, deaths) = game.kDCount()
        (old.first + kills) to (old.second + deaths)
    }

    private fun Iterable<KDWithName>.kDCount(): Pair<Int, Int> {
        var kills = 0
        var deaths = 0
        forEach {
            kills += it.kills
            deaths += it.deaths
        }
        return kills to deaths
    }

    private suspend fun generateDescription(
        game: List<List<KDWithName>>,
        spoiler: Boolean,
        is4v4: Boolean,
        kLang: K18nLanguage,
        pokemonLang: Language?,
        guildId: Long,
        playerNames: List<String>,
        defaultNameLookup: Map<ShowdownID, String>
    ): String {
        val displayNames = if (pokemonLang == null) emptyMap() else displayService.getDisplayNamesOfReplay(
            game.flatten().mapTo(mutableSetOf()) { it.name }, guildId, pokemonLang
        )
        val monStrings = game.map { player ->
            val notAllDead = !player.all { it.deaths > 0 }
            player.joinToString("\n") { mon ->
                val pokemonName = displayNames[mon.name] ?: defaultNameLookup[mon.name] ?: mon.name.value
                buildString {
                    append(pokemonName)
                    if (mon.kills > 0) append(" ${mon.kills}")
                    if (mon.deaths > 0 && (notAllDead || spoiler)) append(" X")
                }
            }
        }
        val allDead = K18n_Analysis.AllDead.translateTo(kLang)
        val description = buildString {
            game.mapIndexed { index, sdPlayer ->
                val number = sdPlayer.count { it.deaths == 0 }.minus(if (is4v4) 2 else 0)
                if (index % 2 > 0) {
                    append(number)
                    if (spoiler) append("||")
                    append(" ")
                    append(playerNames[index])
                } else {
                    append(playerNames[index])
                    append(" ")
                    if (spoiler) append("||")
                    append(number)
                }
                if(index != game.lastIndex) append(":")
            }
            if (is4v4) append("\n(4v4)")
            append("\n\n")
            game.forEachIndexed { index, player ->
                append(playerNames[index])
                append(":")
                if (player.all { it.deaths > 0 } && !spoiler) append(allDead)
                append("\n")
                if (spoiler) append("||")
                append(monStrings[index])
                if (spoiler) append("||")
                if (index < game.lastIndex) append("\n\n")
            }
        }
        return description
    }
}
