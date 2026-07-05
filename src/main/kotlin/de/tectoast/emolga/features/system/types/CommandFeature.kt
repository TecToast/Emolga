package de.tectoast.emolga.features.system.types

import de.tectoast.emolga.discord.jda.features.JDAInteractionData
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.ArgSpec
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.CommandSpec
import de.tectoast.emolga.features.system.model.GuildChecker
import de.tectoast.emolga.features.system.model.NotAllowed
import de.tectoast.emolga.features.system.nameToDiscordOption
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.generic.K18n_TooManyResults
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import org.koin.core.component.inject

abstract class CommandFeature<A : Arguments>(argsFun: () -> A, spec: CommandSpec) :
    Feature<CommandSpec, SlashCommandInteractionEvent, A>(
        argsFun, spec, SlashCommandInteractionEvent::class, eventToName
    ) {
    private val autoCompletableOptions by lazy {
        defaultArgs.mapNotNull { (it.spec as? CommandArgSpec)?.autocomplete?.let { ac -> it.name.nameToDiscordOption() to ac } }
            .toMap()
    }
    open val children: List<CommandFeature<*>> = emptyList()
    val childCommands by lazy { children.associateBy { it.spec.name } }
    var slashPermissions: DefaultMemberPermissions = DefaultMemberPermissions.ENABLED

    val botConstants by inject<BotConstants>()

    init {
        val languageRepo: GuildConfigRepository by inject()
        registerListener<CommandAutoCompleteInteractionEvent> {
            if (it.name != spec.name && it.name != it.subcommandName) return@registerListener
            permissionCheck(JDAInteractionData(it), botConstants.botOwnerId).let { result ->
                if (result is NotAllowed) {
                    return@registerListener it.replyChoiceStrings(result.reason.translateTo(languageRepo.getLanguage(it.guild?.idLong)))
                        .queue()
                }
            }
            val focusedOption = it.focusedOption
            var cmd: CommandFeature<*> = this
            it.subcommandGroup?.let { group ->
                cmd = cmd.childCommands[group] ?: return@registerListener
            }
            it.subcommandName?.let { name ->
                cmd = cmd.childCommands[name] ?: return@registerListener
            }
            val options = cmd.autoCompletableOptions
            options[focusedOption.name]?.let { ac ->
                val list = ac(focusedOption.value, it)?.takeIf { l -> l.size <= 25 }
                it.replyChoiceStrings(
                    list ?: listOf(K18n_TooManyResults.translateTo(languageRepo.getLanguage(it.guild?.idLong)))
                )
                    .queue()
            }
        }
    }

    override suspend fun populateArgs(data: InteractionData, e: SlashCommandInteractionEvent, args: A) {
        populateArgs(data, args.args) { str, _ ->
            e.getOption(str)
        }
    }

    protected fun slashPrivate() {
        slashPermissions = DefaultMemberPermissions.DISABLED
    }

    companion object {
        val eventToName: (SlashCommandInteractionEvent) -> String = { it.name }
    }
}

data class CommandArgSpec(
    val autocomplete: (suspend (String, CommandAutoCompleteInteractionEvent) -> List<String>?)? = null,
    val choices: List<Choice>? = null,
    val guildChecker: GuildChecker? = null
) : ArgSpec