@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.features

import de.tectoast.emolga.K18n_FeatureManager
import de.tectoast.emolga.database.exposed.CommandManagementRepository
import de.tectoast.emolga.database.exposed.CommandManagementService
import de.tectoast.emolga.database.exposed.GuildLanguageRepository
import de.tectoast.emolga.di.StartupTask
import de.tectoast.emolga.features.flo.AddRemove
import de.tectoast.emolga.utils.*
import de.tectoast.k18n.generated.K18nLanguage
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.koin.core.annotation.Single
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

interface FeatureEventHandler {
    suspend fun handleEvent(e: GenericEvent)
}

interface CommandRegistryService {
    suspend fun updateCommandsForGuild(gid: Long)

    suspend fun modifyGuildGroup(guildId: Long, group: String, action: AddRemove)

    suspend fun modifyGuildCommand(guildId: Long, command: String, action: AddRemove)

    suspend fun modifyGroupCommand(group: String, command: String, action: AddRemove)
}

interface JDAProvider {
    fun provideJDA(guild: Long): JDA
}

@Single
class ProductionJDAProvider(val emolgaJDA: JDA, val flegmonJDA: JDA) : JDAProvider {
    override fun provideJDA(guild: Long) = if (guild == Constants.G.PEPE) flegmonJDA else emolgaJDA
}

@Single
class FeatureRegistry(
    val loadListeners: Set<ListenerProvider>,
) {
    val featureList = loadListeners.filterIsInstance<Feature<*, *, *>>()
    val registeredFeatureList = featureList.filter { it.spec is RegisteredFeatureSpec }
    val featureNames = registeredFeatureList.map { it.spec.name }
}

@Single
class JDACommandRegistryService(
    val cmdManagementService: CommandManagementService,
    val cmdManagementRepo: CommandManagementRepository,
    val jdaProvider: JDAProvider,
    val featureRegistry: FeatureRegistry,
) : CommandRegistryService, StartupTask {
    private val logger = KotlinLogging.logger {}

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
        setDescriptionLocalizations(cmd.spec.help.toDiscordLocaleMap())
        setContexts(if (cmd.spec.inDM) InteractionContextType.BOT_DM else InteractionContextType.GUILD)
        if (cmd.children.isNotEmpty()) {
            cmd.children.forEach {
                addSubcommands(
                    SubcommandData(
                        it.spec.name,
                        it.spec.help.translateTo(K18nLanguage.EN)
                    ).setDescriptionLocalizations(it.spec.help.toDiscordLocaleMap()).addOptions(
                        generateOptionData(
                            it, gid
                        )
                    )
                )
            }
        } else addOptions(generateOptionData(cmd, gid))
    }

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
                when (it(CommandProviderData(gid))) {
                    ArgumentPresence.NOT_PRESENT -> return@mapNotNull null
                    ArgumentPresence.REQUIRED -> true
                    ArgumentPresence.OPTIONAL -> false
                }
            } ?: !a.optional
            OptionData(
                a.optionType,
                a.name.nameToDiscordOption(),
                a.help.translateToGuildLanguage(gid),
                required,
                spec?.autocomplete != null
            ).apply {
                if (spec?.choices != null) addChoices(spec.choices)
            }
        }
    }
}

@Single
class JDAFeatureEventHandler(
    featureRegistry: FeatureRegistry,
    private val guildLanguageRepo: GuildLanguageRepository,
) : FeatureEventHandler {
    private val listenerScope = createCoroutineScope("FeatureManagerListener")
    private val surveillanceScope = createCoroutineScope("FeatureManagerSurveillance")

    private val eventToName: Map<KClass<*>, (GenericInteractionCreateEvent) -> String>
    private val features: Map<KClass<*>, Map<String, Feature<*, *, Arguments>>>
    private val listeners: Map<KClass<out GenericEvent>, List<suspend (GenericEvent) -> Unit>>


    init {
        val featureList = featureRegistry.featureList
        eventToName = featureList.associate {
            it.eventClass to it.eventToName
        }
        features = featureList.groupBy { it.eventClass }
            .mapValues {
                buildMap {
                    it.value.forEach { v ->
                        for (name in listOf(v.spec.name) + v.spec.aliases) {
                            if (name in this) logger.warn { "Feature $name is registered multiple times for event ${it.key.simpleName}! This may cause issues!" }
                            this[name] = v as Feature<*, GenericInteractionCreateEvent, Arguments>
                        }
                    }
                }
            }
        listeners = featureRegistry.loadListeners.flatMap { it.registeredListeners }.groupBy { it.first }.mapValues {
            it.value.map { v -> v.second }
        }
    }

    override suspend fun handleEvent(e: GenericEvent) {
        logger.trace { "Handling ${e::class.simpleName}" }
        val kClass = e::class
        listeners[kClass]?.forEach {
            listenerScope.launch {
                it(e)
            }
        }
        if (e is GenericInteractionCreateEvent) {
            val eventFeatures = features[kClass] ?: return
            val feature = eventFeatures[eventToName[kClass]!!(e)] ?: return
            execute(
                if (e is SlashCommandInteractionEvent && feature is CommandFeature<*> && feature.children.isNotEmpty()) feature.childCommands[e.subcommandName]!! as Feature<*, *, Arguments>
                else feature, e
            )
        }
    }

    private suspend fun execute(
        feature: Feature<*, GenericInteractionCreateEvent, Arguments>,
        e: GenericInteractionCreateEvent,
    ) {
        val language = guildLanguageRepo.getLanguage(e.guild?.idLong)
        val data = RealInteractionData(e, language)
        surveillanceScope.launch {
            val executionStart = TimeSource.Monotonic.markNow()
            withTimeoutOrNull(10.seconds) {
                data.acknowledged.await()
            }
            val duration = executionStart.elapsedNow()
            if (duration > 2.seconds)
                logger.warn(
                    "Interaction not acknowledged after 2s (needed ${executionStart.elapsedNow()}): ${buildLogMessage(e)}"
                )
        }
        with(data) {
            try {
                logger.info(buildLogMessage(e))
                when (val result = feature.permissionCheck(data)) {
                    Allowed -> {
                        val args = feature.argsFun()
                        try {
                            feature.populateArgs(data, e, args)
                        } catch (ex: ArgumentException) {
                            data.reply(
                                ex.msg.t() + "\n" + K18n_FeatureManager.IfErrorContact(Constants.MYTAG).t(),
                                ephemeral = true
                            )
                            return
                        }
                        feature.exec(args)
                    }

                    is NotAllowed -> {
                        reply(result.reason, ephemeral = true)
                    }
                }
            } catch (ex: Exception) {
                reply(
                    K18n_FeatureManager.InteractionError,
                    ephemeral = true
                )
                logger.error(
                    buildErrorMessage(feature, e, data, ex)
                )
            }
        }
    }

    private fun buildLogMessage(
        e: GenericInteractionCreateEvent
    ) = "${e.stringify()} by ${e.user.name} in ${e.channel?.name} in ${e.guild?.name ?: "DM"}"

    private fun buildErrorMessage(
        feature: Feature<*, GenericInteractionCreateEvent, Arguments>,
        e: GenericInteractionCreateEvent,
        data: RealInteractionData,
        ex: Exception
    ) =
        ("Error in feature ${feature.spec.name}:\n" + "Event: ${e::class.simpleName}\n" + "User: ${data.user}\n" + (if (data.gid != -1L) {
            "Guild: ${data.gid} [${data.textChannel.guild.name}]\n" + "Channel: ${data.tc} [${data.textChannel.name}]\n"
        } else "" + "Input: ${e.stringify()}\n") + ex.stackTraceToString())

    private fun GenericInteractionCreateEvent.stringify() = when (this) {
        is SlashCommandInteractionEvent -> this.commandString
        is ModalInteractionEvent -> "M:${this.modalId}"
        is GenericComponentInteractionCreateEvent -> "C:${this.componentId}"
        else -> this.toString()
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

inline fun <reified T> Iterable<*>.filterIsInstance(filter: (T) -> Boolean): List<T> {
    val result = ArrayList<T>()
    for (element in this) if (element is T && filter(element)) result.add(element)
    return result
}
