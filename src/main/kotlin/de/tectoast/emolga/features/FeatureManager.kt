@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.features

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.K18n_FeatureManager
import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.CmdManager
import de.tectoast.emolga.database.exposed.GuildLanguageDB
import de.tectoast.emolga.utils.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import mu.KotlinLogging
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
import kotlin.reflect.KClass
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class FeatureManager(private val loadListeners: Set<ListenerProvider>) {
    private val listenerScope = createCoroutineScope("FeatureManagerListener")
    private val surveillanceScope = createCoroutineScope("FeatureManagerSurveillance")

    constructor(packageName: String) : this(packageName.let {
        ClassPath.from(Thread.currentThread().contextClassLoader).getTopLevelClassesRecursive(packageName).mapNotNull {
            findAllFeaturesRecursively(it.load().kotlin)
        }.flatten().toSet()
    })

    private val eventToName: Map<KClass<*>, (GenericInteractionCreateEvent) -> String>
    private val features: Map<KClass<*>, Map<String, Feature<*, *, Arguments>>>
    private val listeners: Map<KClass<out GenericEvent>, List<suspend (GenericEvent) -> Unit>>

    val featureList get() = loadListeners.filterIsInstance<Feature<*, *, *>>()
    val registeredFeatureList get() = featureList.filter { it.spec is RegisteredFeatureSpec }

    init {
        val featuresSet = featureList
        eventToName = featuresSet.associate {
            @Suppress("USELESS_CAST") // compiler bug
            it.eventClass to (it.eventToName as (GenericInteractionCreateEvent) -> String)
        }
        features = featuresSet.groupBy { it.eventClass }
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
        listeners = loadListeners.flatMap { it.registeredListeners }.groupBy { it.first }.mapValues {
            it.value.map { v -> v.second }
        }
    }


    suspend fun handleEvent(e: GenericEvent) {
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
        val language = GuildLanguageDB.getLanguage(e.guild?.idLong)
        val data = RealInteractionData(e, language)
        surveillanceScope.launch {
            val executionStart = TimeSource.Monotonic.markNow()
            withTimeoutOrNull(10000) {
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

    private suspend fun buildSlashCommandData(
        cmd: CommandFeature<*>,
        gid: Long
    ) = Commands.slash(cmd.spec.name, cmd.spec.help.default()).apply {
        defaultPermissions = cmd.slashPermissions
        setDescriptionLocalizations(cmd.spec.help.toDiscordLocaleMap())
        setContexts(if (cmd.spec.inDM) InteractionContextType.BOT_DM else InteractionContextType.GUILD)
        if (cmd.children.isNotEmpty()) {
            cmd.children.forEach {
                addSubcommands(
                    SubcommandData(
                        it.spec.name,
                        it.spec.help.default()
                    ).setDescriptionLocalizations(it.spec.help.toDiscordLocaleMap()).addOptions(
                        generateOptionData(
                            it, gid
                        )
                    )
                )
            }
        } else addOptions(generateOptionData(cmd, gid))
    }

    suspend fun updateCommandsForGuild(gid: Long) {
        val guildFeatures: MutableSet<CommandData> = mutableSetOf()
        val allFeatureNames = CmdManager.getFeaturesForGuild(gid)
        with(guildFeatures) {
            loadListeners.filterIsInstance<Feature<*, *, *>> { it.spec.name in allFeatureNames }.forEach { cmd ->
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
        val usedJda = if (gid == Constants.G.PEPE) EmolgaMain.flegmonjda!! else jda
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

    companion object {
        private val logger = KotlinLogging.logger {}

        fun findAllFeaturesRecursively(cl: KClass<*>): List<ListenerProvider> {
            val o = cl.objectInstance ?: return emptyList()
            return if (o is ListenerProvider) listOf(o) else {
                cl.nestedClasses.flatMap { findAllFeaturesRecursively(it) }
            }
        }
    }
}

inline fun <reified T> Iterable<*>.filterIsInstance(filter: (T) -> Boolean): List<T> {
    val result = ArrayList<T>()
    for (element in this) if (element is T && filter(element)) result.add(element)
    return result
}
