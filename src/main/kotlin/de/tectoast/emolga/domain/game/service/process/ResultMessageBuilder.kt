package de.tectoast.emolga.domain.game.service.process

import de.tectoast.emolga.domain.config.model.GuildConfigType
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.domain.game.model.KDWithName
import de.tectoast.emolga.domain.game.model.ResultMessage
import de.tectoast.emolga.domain.game.model.SingleGame
import de.tectoast.emolga.domain.league.tierlist.repository.TierlistRepository
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
        game: SingleGame,
        language: K18nLanguage,
        dontTranslateFromReplayServer: Boolean,
        playerNames: List<String>,
        gid: Long,
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
        val sanitizedPlayerNames = playerNames.map { it.replace("_", "\\_").replace("*", "\\*") }
        val description = generateDescription(
            game = game,
            spoiler = spoiler,
            kLang = language,
            pokemonLang = pokemonLang,
            guildId = gid,
            sanitizedPlayerNames = sanitizedPlayerNames,
        )
        val resultMessages = mutableListOf<ResultMessage>()
        resultMessages += ResultMessage.Game(description)
        var illusionMonPresent = false
        for ((index, ga) in game.kd.withIndex()) {
            if (ga.containsIllusionMon()) {
                resultMessages += ResultMessage.IllusionWarning(sanitizedPlayerNames[index])
                illusionMonPresent = true
            }
        }
        if (gid != botConstants.botOwnerGuildId && game.kd.totalKDCount().let { it.first != it.second }) {
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
        game: SingleGame,
        spoiler: Boolean,
        kLang: K18nLanguage,
        pokemonLang: Language?,
        guildId: Long,
        sanitizedPlayerNames: List<String>
    ): String {
        val kd = game.kd
        val displayNames = if (pokemonLang == null) emptyMap() else displayService.getDisplayNamesOfReplay(
            kd.flatten().mapTo(mutableSetOf()) { it.name }, guildId, pokemonLang
        )
        val allDead = K18n_Analysis.AllDead.translateTo(kLang)
        val description = buildString {
            kd.mapIndexed { index, sdPlayer ->
                val list = buildList {
                    val shouldMarkWinner = game.winnerIndex == index
                    add(sanitizedPlayerNames[index])
                    add(" ")
                    if (spoiler) add("||")
                    add(buildString {
                        if (shouldMarkWinner) append("__")
                        append(sdPlayer.count { it.deaths == 0 }.minus(if (game.is4v4) 2 else 0))
                        if (shouldMarkWinner) append("__")
                    })
                }
                val isOddIndex = index % 2 > 0
                for (item in if (isOddIndex) list.reversed() else list) append(item)
                if (!isOddIndex) append(":")
            }
            if (game.is4v4) append("\n(4v4)")
            append("\n\n")
            kd.forEachIndexed { index, player ->
                append(sanitizedPlayerNames[index])
                append(":")
                if (player.all { it.deaths > 0 } && !spoiler) append(allDead)
                append("\n")
                if (spoiler) append("||")
                val notAllDead = !player.all { it.deaths > 0 }
                append(player.joinToString("\n") { mon ->
                    val pokemonName = displayNames[mon.name] ?: game.defaultNameLookup[mon.name] ?: mon.name.value
                    buildString {
                        append(pokemonName)
                        if (mon.kills > 0) append(" ${mon.kills}")
                        if (mon.deaths > 0 && (notAllDead || spoiler)) append(" X")
                    }
                })
                if (spoiler) append("||")
                if (index < kd.lastIndex) append("\n\n")
            }
        }
        return description
    }
}
