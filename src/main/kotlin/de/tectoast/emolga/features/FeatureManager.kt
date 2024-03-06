@file:Suppress("UNCHECKED_CAST")

package de.tectoast.emolga.features

import com.google.common.reflect.ClassPath
import de.tectoast.emolga.features.flo.SendFeatures
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.annotations.ToTest
import de.tectoast.emolga.utils.condAppend
import de.tectoast.emolga.utils.createCoroutineScope
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

class FeatureManager(private val loadListeners: Set<ListenerProvider>) {
    private val listenerScope = createCoroutineScope("FeatureManagerListener")

    constructor(packageName: String) : this(packageName.let {
        ClassPath.from(Thread.currentThread().contextClassLoader).getTopLevelClassesRecursive(packageName).mapNotNull {
            val cl = it.load().kotlin
            if (cl.hasAnnotation<ToTest>()) {
                logger.warn("Feature ${cl.simpleName} needs to be tested!")
            }
            findAllFeaturesRecursively(cl)
        }.flatten().toSet()
    })

    private val eventToName: Map<KClass<*>, (GenericInteractionCreateEvent) -> String>
    private val features: Map<KClass<*>, Map<String, Feature<*, *, Arguments>>>
    private val listeners: Map<KClass<out GenericEvent>, List<suspend (GenericEvent) -> Unit>>

    init {
        val featuresSet = loadListeners.filterIsInstance<Feature<*, *, *>>()
        eventToName = featuresSet.associate {
            @Suppress("USELESS_CAST") // compiler bug
            it.eventClass to (it.eventToName as (GenericInteractionCreateEvent) -> String)
        }
        features = featuresSet.groupBy { it.eventClass }
            .mapValues { it.value.associate { k -> k.spec.name to k as Feature<*, GenericInteractionCreateEvent, Arguments> } }
        listeners = loadListeners.flatMap { it.registeredListeners }.groupBy { it.first }.mapValues {
            it.value.map { v -> v.second }
        }
    }


    suspend fun handleEvent(e: GenericEvent) {
        logger.debug { "Handling ${e::class.simpleName}" }
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
        val data = RealInteractionData(e)
        with(data) {
            try {
                when (val result = feature.permissionCheck(data)) {
                    Allowed -> {
                        val args = feature.argsFun()
                        try {
                            feature.populateArgs(data, e, args)
                        } catch (ex: ArgumentException) {
                            data.reply(
                                ex.message + "\nWenn du denkst, dass dies ein Fehler ist, melde dich bitte bei ${Constants.MYTAG}.",
                                ephemeral = true
                            )
                            return
                        }
                        logger.info(buildLogMessage(e))
                        feature.exec(args)
                    }

                    is NotAllowed -> {
                        reply(result.reason, ephemeral = true)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                reply(
                    "Es ist ein Fehler beim Ausführen der Interaktion aufgetreten!\nWenn du denkst, dass dies ein interner Fehler beim Bot ist, melde dich bitte bei Flo (${Constants.MYTAG}).".condAppend(
                        data.user == Constants.FLOID, "\nJa Flo, du sollst dich auch bei ihm melden du Kek :^)"
                    ),
                    ephemeral = true
                )
                SendFeatures.sendToMe(
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
        ("Error in feature ${feature.spec.name}:\n" + "Event: ${e::class.simpleName}\n" + "User: `${data.user}`\n" + (if (data.gid != -1L) {
            "Guild: `${data.gid}` [${data.textChannel.guild.name}]\n" + "Channel: `${data.tc}` [${data.textChannel.asMention}]\n"
        } else "" + "Input: ```${e.stringify()}```\n```") + ex.stackTraceToString()).take(1997) + "```"

    private fun GenericInteractionCreateEvent.stringify() = when (this) {
        is SlashCommandInteractionEvent -> this.commandString
        is GenericComponentInteractionCreateEvent -> this.componentId
        is ModalInteractionEvent -> this.modalId
        else -> this.toString()
    }

    suspend fun updateFeatures(jda: JDA, updateGuilds: List<Long>? = null) {
        val guildSlashFeatures: MutableMap<Long, MutableSet<SlashCommandData>> = mutableMapOf()

        loadListeners.filterIsInstance<CommandFeature<*>>().forEach { cmd ->
            (cmd.spec.guilds + Constants.G.MY).forEach { gid ->
                val data = Commands.slash(cmd.spec.name, cmd.spec.help).apply {
                    defaultPermissions = cmd.slashPermissions
                    isGuildOnly = true
                    if (cmd.children.isNotEmpty()) {
                        cmd.children.forEach {
                            addSubcommands(
                                SubcommandData(it.spec.name, it.spec.help).addOptions(generateOptionData(it, gid))
                            )
                        }
                    } else addOptions(generateOptionData(cmd, gid))
                }
                guildSlashFeatures.getOrPut(gid) { mutableSetOf() }.add(data)
            }
        }

        for (gid in updateGuilds ?: db.config.only().guildsToUpdate.ifEmpty { guildSlashFeatures.keys }) {
            val commands = guildSlashFeatures[gid] ?: continue
            (if (gid == -1L) jda.updateCommands()
            else jda.getGuildById(gid)?.updateCommands())?.addCommands(commands)?.queue()
        }
    }

    private fun generateOptionData(feature: CommandFeature<*>, gid: Long) = feature.defaultArgs.mapNotNull { a ->
        if (a.onlyInCode) return@mapNotNull null
        val spec = a.spec as? CommandArgSpec
        if (spec?.disabledGuilds?.let { it.isNotEmpty() && gid in it } == true) return@mapNotNull null
        OptionData(a.optionType, a.name.nameToDiscordOption(), a.help, !a.optional, spec?.autocomplete != null).apply {
            if (spec?.choices != null) addChoices(spec.choices)
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

