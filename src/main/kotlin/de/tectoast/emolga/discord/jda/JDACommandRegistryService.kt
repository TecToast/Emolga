package de.tectoast.emolga.discord.jda

import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.discord.jda.provider.JDAProvider
import de.tectoast.emolga.domain.cmdmanage.model.AddRemove
import de.tectoast.emolga.domain.cmdmanage.repository.CommandManagementRepository
import de.tectoast.emolga.domain.cmdmanage.service.CommandManagementService
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.features.system.CommandRegistryService
import de.tectoast.emolga.features.system.FeatureRegistry
import de.tectoast.emolga.features.system.model.ArgumentPresence
import de.tectoast.emolga.features.system.nameToDiscordOption
import de.tectoast.emolga.features.system.types.CommandArgSpec
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.MessageContextFeature
import de.tectoast.emolga.utils.toDiscordLocaleMap
import de.tectoast.k18n.generated.K18nLanguage
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.*
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class JDACommandRegistryService(
    private val cmdManagementService: CommandManagementService,
    private val cmdManagementRepo: CommandManagementRepository,
    private val jdaProvider: JDAProvider,
    private val languageRepo: GuildConfigRepository,
) : CommandRegistryService, StartupTask, KoinComponent {
    private val logger = KotlinLogging.logger {}

    private val featureRegistry: FeatureRegistry by inject()

    override suspend fun onStartup() {
        val registeredFeatureNames = featureRegistry.registeredFeatureList.map { it.spec.name }.toSet()
        val updatedGuilds = cmdManagementService.startupCheck(registeredFeatureNames)
        for (gid in updatedGuilds) {
            updateCommandsForGuild(gid)
        }
    }

    override suspend fun modifyGuildGroup(
        guildId: Long,
        group: String,
        action: AddRemove
    ) {
        cmdManagementService.modifyGuildGroup(guildId, group, action)
        updateCommandsForGuild(guildId)
    }

    override suspend fun modifyGuildCommand(
        guildId: Long,
        command: String,
        action: AddRemove
    ) {
        cmdManagementService.modifyGuildCommand(guildId, command, action)
        updateCommandsForGuild(guildId)
    }

    override suspend fun modifyGroupCommand(
        group: String,
        command: String,
        action: AddRemove
    ) {
        cmdManagementService.modifyGroupCommand(group, command, action)
        updateAllGuildsInGroup(group)
    }

    private suspend fun updateAllGuildsInGroup(group: String) {
        for (guild in cmdManagementRepo.getGuildsForGroup(group)) {
            updateCommandsForGuild(guild)
        }
    }

    private suspend fun buildSlashCommandData(
        cmd: CommandFeature<*>,
        gid: Long
    ) = Commands.slash(cmd.spec.name, cmd.spec.help.translateTo(K18nLanguage.EN)).apply {
        defaultPermissions = cmd.slashPermissions
        setContexts(if (cmd.spec.inDM) InteractionContextType.BOT_DM else InteractionContextType.GUILD)

        setDescriptionLocalizations(cmd.spec.help.toDiscordLocaleMap())
        if (cmd.children.isNotEmpty()) {
            cmd.children.forEach {
                if (it.children.isNotEmpty()) {
                    val group = SubcommandGroupData(
                        it.spec.name,
                        it.spec.help.translateTo(K18nLanguage.EN)
                    ).setDescriptionLocalizations(it.spec.help.toDiscordLocaleMap())
                    it.children.forEach { child -> group.addSubcommands(createSubCommandData(child, gid)) }
                    addSubcommandGroups(group)
                } else {
                    addSubcommands(createSubCommandData(it, gid))
                }
            }
        } else addOptions(generateOptionData(cmd, gid))
    }

    private suspend fun createSubCommandData(
        feature: CommandFeature<*>,
        gid: Long
    ) = SubcommandData(
        feature.spec.name,
        feature.spec.help.translateTo(K18nLanguage.EN)
    ).setDescriptionLocalizations(feature.spec.help.toDiscordLocaleMap()).addOptions(
        generateOptionData(
            feature, gid
        )
    )

    override suspend fun updateCommandsForGuild(gid: Long) {
        val guildFeatures: MutableSet<CommandData> = mutableSetOf()
        val allFeatureNames = cmdManagementRepo.getFeaturesForGuild(gid)
        with(guildFeatures) {
            featureRegistry.featureList.filter { it.spec.name in allFeatureNames }.forEach { cmd ->
                when (cmd) {
                    is CommandFeature<*> -> {
                        add(buildSlashCommandData(cmd, gid))
                    }

                    is MessageContextFeature -> {
                        add(Commands.message(cmd.spec.name))
                    }

                    else -> {}
                }
            }
        }
        val usedJda = jdaProvider.provideJDA(gid)
        (if (gid == -1L) usedJda.updateCommands()
        else usedJda.getGuildById(gid)?.updateCommands())?.addCommands(guildFeatures)?.queue()
    }

    private suspend fun generateOptionData(feature: CommandFeature<*>, gid: Long): List<OptionData> {
        logger.debug { "Generating options for ${feature.spec.name}" }
        return feature.defaultArgs.mapNotNull { a ->
            if (a.onlyInCode) return@mapNotNull null
            val spec = a.spec as? CommandArgSpec
            val required = spec?.guildChecker?.let {
                when (it(gid)) {
                    ArgumentPresence.NOT_PRESENT -> return@mapNotNull null
                    ArgumentPresence.REQUIRED -> true
                    ArgumentPresence.OPTIONAL -> false
                }
            } ?: !a.optional
            OptionData(
                a.optionType,
                a.name.nameToDiscordOption(),
                a.help.translateTo(languageRepo.getLanguage(gid)),
                required,
                spec?.autocomplete != null
            ).apply {
                if (spec?.choices != null) addChoices(spec.choices)
            }
        }
    }
}