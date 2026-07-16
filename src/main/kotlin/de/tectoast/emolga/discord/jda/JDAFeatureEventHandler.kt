package de.tectoast.emolga.discord.jda

import de.tectoast.emolga.K18n_FeatureManager
import de.tectoast.emolga.discord.jda.features.JDAInteractionData
import de.tectoast.emolga.domain.config.repository.GuildConfigRepository
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.FeatureEventHandler
import de.tectoast.emolga.features.system.FeatureRegistry
import de.tectoast.emolga.features.system.model.Allowed
import de.tectoast.emolga.features.system.model.ArgumentException
import de.tectoast.emolga.features.system.model.NotAllowed
import de.tectoast.emolga.features.system.types.CommandFeature
import de.tectoast.emolga.features.system.types.Feature
import de.tectoast.emolga.utils.BotConstants
import de.tectoast.emolga.utils.t
import kotlinx.coroutines.*
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import org.koin.core.annotation.Single
import org.slf4j.MDC
import kotlin.reflect.KClass
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Suppress("UNCHECKED_CAST")
@Single
class JDAFeatureEventHandler(
    private val guildLanguageRepo: GuildConfigRepository,
    private val botConstants: BotConstants,
    private val clock: Clock,
    featureRegistry: FeatureRegistry,
    baseListenerScope: CoroutineScope,
    baseSurveillanceScope: CoroutineScope
) : FeatureEventHandler {
    private val listenerScope = baseListenerScope + CoroutineName("FeatureManagerListener")
    private val surveillanceScope = baseSurveillanceScope + CoroutineName("FeatureManagerSurveillance")

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
            if (e !is SlashCommandInteractionEvent) execute(feature, e)
            else {
                var cmd = feature as? CommandFeature<*> ?: return
                e.subcommandGroup?.let { groupName ->
                    cmd = cmd.childCommands[groupName] ?: return
                }
                e.subcommandName?.let { name ->
                    cmd = cmd.childCommands[name] ?: return
                }
                execute(
                    cmd as Feature<*, *, Arguments>, e
                )
            }
        }
    }

    private suspend fun execute(
        feature: Feature<*, GenericInteractionCreateEvent, Arguments>,
        e: GenericInteractionCreateEvent,
    ) {
        val language = guildLanguageRepo.getLanguage(e.guild?.idLong)
        val data = JDAInteractionData(e, language)
        val traceId = clock.now().toEpochMilliseconds().toString()
        MDC.put("traceId", traceId)
        surveillanceScope.launch(MDCContext()) {
            val executionStart = TimeSource.Monotonic.markNow()
            withTimeoutOrNull(10.seconds) {
                data.acknowledged.await()
            }
            val duration = executionStart.elapsedNow()
            if (duration > 2.seconds)
                logger.warn(
                    "Interaction not acknowledged after 2s (needed ${executionStart.elapsedNow()})}"
                )
        }
        with(data) {
            try {
                logger.atInfo()
                    .setMessage("Invocation")
                    .addKeyValue("guild", e.guild?.idLong ?: 0)
                    .addKeyValue("channel", e.channel?.idLong ?: 0)
                    .addKeyValue("user", e.user.idLong)
                    .addKeyValue("interaction", e.stringify())
                    .log()

                withContext(MDCContext()) {
                    when (val result = feature.permissionCheck(data, botConstants.botOwnerId)) {
                        Allowed -> {
                            val args = feature.argsFun()
                            try {
                                feature.populateArgs(data, e, args)
                            } catch (ex: ArgumentException) {
                                data.replyRaw(
                                    ex.msg.t() + "\n" + K18n_FeatureManager.IfErrorContact(botConstants.botOwnerTag)
                                        .t(),
                                    ephemeral = true
                                )
                                return@withContext
                            }
                            logger.atInfo().setMessage("Feature").addKeyValue("feature", feature.spec.name)
                                .addKeyValue("args", args.toMap()).log()
                            feature.exec(args)
                        }

                        is NotAllowed -> {
                            reply(result.reason, ephemeral = true)
                        }
                    }
                }
            } catch (ex: CancellationException) {
                throw ex
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
        data: JDAInteractionData,
        ex: Exception
    ) =
        ("Error in feature ${feature.spec.name}:\n" + "Event: ${e::class.simpleName}\n" + "User: ${data.user}\n" + (if (data.gid != -1L) {
            "Guild: ${data.gid} [${data.e.guild?.name}]\n" + "Channel: ${data.tc} [${data.e.channel?.name}]\n"
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
