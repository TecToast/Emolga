package de.tectoast.emolga.features.system.types

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.features.system.Arg
import de.tectoast.emolga.features.system.Arguments
import de.tectoast.emolga.features.system.FeatureSpec
import de.tectoast.emolga.features.system.model.*
import de.tectoast.emolga.features.system.nameToDiscordOption
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import kotlin.reflect.KClass

abstract class Feature<out T : FeatureSpec, out E : GenericInteractionCreateEvent, A : Arguments>(
    val argsFun: () -> @UnsafeVariance A,
    val spec: T,
    val eventClass: KClass<@UnsafeVariance E>,
    val eventToName: (@UnsafeVariance E) -> String
) : ListenerProvider() {
    private var checkSpecial: AllowedResultCheck = { Allowed }
    private var check: BooleanCheck = { true }

    val defaultArgs by lazy { argsFun().args }
    abstract suspend fun populateArgs(data: InteractionData, e: @UnsafeVariance E, args: A)

    fun createComponentId(argsBuilder: ArgBuilder<@UnsafeVariance A>, checkCompId: Boolean = false) =
        spec.name + ";" + argsFun().apply(argsBuilder).args.filter { !checkCompId || it.compIdOnly }
            .joinToString(";") { it.parsed?.toString() ?: "" }

    private fun buildArgs(argsBuilder: ArgBuilder<A>) = argsFun().apply(argsBuilder)

    protected suspend inline fun populateArgs(
        data: InteractionData, args: List<Arg<*, *>>, parser: (String, Int) -> Any?
    ) {
        for ((index, arg) in args.withIndex()) {
            val m = parser(arg.name.nameToDiscordOption(), index)
            if (m == null && !arg.optional) {
                throw MissingArgumentException(arg)
            }
            if (m != null) arg.parse(data, m)
        }
    }

    suspend fun permissionCheck(data: InteractionData, botOwnerId: Long) = if (data.user == botOwnerId) Allowed else {
        if (!check(data)) NotAllowed.Companion else checkSpecial(data)
    }

    fun restrict(check: BooleanCheck) {
        this.check = check
    }

    fun restrictResult(check: AllowedResultCheck) {
        this.checkSpecial = check
    }

    fun user(uid: Long): BooleanCheck = {
        user == uid
    }

    val admin: BooleanCheck = {
        data.adminOnGuild
    }
    val flo: BooleanCheck = { false } // flo may use any feature regardless of this configuration

    context (iData: InteractionData)
    abstract suspend fun exec(e: A)

    context(iData: InteractionData)
    suspend fun exec(argsBuilder: ArgBuilder<@UnsafeVariance A> = {}) =
        exec(buildArgs(argsBuilder))
}