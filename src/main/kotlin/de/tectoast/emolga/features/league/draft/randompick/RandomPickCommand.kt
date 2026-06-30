package de.tectoast.emolga.features.league.draft.randompick

import de.tectoast.emolga.domain.league.config.service.GuildConfigService
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickArgument
import de.tectoast.emolga.domain.league.draft.model.random.RandomPickUserInput
import de.tectoast.emolga.domain.league.draft.service.core.DraftService
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.interaction.validationCompleteCallback
import de.tectoast.emolga.features.league.draft.K18n_RandomPick
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.model.ArgumentPresence
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.ListenerProvider
import de.tectoast.emolga.utils.Language
import org.koin.core.annotation.Single
import org.koin.core.component.inject

@Single(binds = [ListenerProvider::class])
class RandomPickCommand(private val draftService: DraftService, private val btn: RandomPickButton) :
    CommandFeature<RandomPickCommand.Args>(::Args, CommandSpec("randompick", K18n_RandomPick.Help)) {
    class Args : Arguments() {
        private val guildConfigService: GuildConfigService by inject()

        var tier by string("tier", K18n_RandomPick.ArgTier) {
            slashCommand(guildChecker = { guild ->
                getArgumentPresence(guild, RandomPickArgument.TIER)
            })
        }.nullable()
        var type by pokemontype("Typ", K18n_RandomPick.ArgType, Language.ENGLISH) {
            slashCommand(guildChecker = { guild ->
                getArgumentPresence(guild, RandomPickArgument.TYPE)
            })
        }.nullable()

        private suspend fun getArgumentPresence(guild: Long, argument: RandomPickArgument): ArgumentPresence {
            val allConfigs = guildConfigService.getAllConfigsForGuild(guild)
            val allPresenceModes = allConfigs.mapTo(mutableSetOf()) { config ->
                config.randomPick.mode.provideCommandOptions()[argument]
                    ?: ArgumentPresence.NOT_PRESENT
            }
            return if (allPresenceModes.size == 1) allPresenceModes.first()
            else ArgumentPresence.OPTIONAL
        }
    }


    context(iData: InteractionData)
    override suspend fun exec(e: Args) {
        iData.deferReply()
        val result = draftService.handleRandomPickRequest(
            input = RandomPickUserInput(
                tier = e.tier,
                type = e.type
            ),
            tcId = iData.tc,
            userId = iData.user,
            roleIds = iData.data.memberRoles,
            validationCompleteCallback = iData.validationCompleteCallback
        )
        handleRandomPickResult(result, btn)
    }
}