package de.tectoast.emolga.features

import de.tectoast.emolga.commands.CommandArgs
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface Feature<T : FeatureSpec> {

}

interface CommandFeature<T : CommandArgs> : Feature<CommandSpec> {
    suspend fun execute()
}

interface FeatureSpec
data class CommandSpec(val name: String, val help: String) : FeatureSpec

open class Arguments {
    val args = mutableListOf<AbstractArg<*>>()
    fun string(name: String, help: String, builder: AbstractArg<String>.() -> Unit = {}) =
        createArg(name, help, builder)

    private fun <T> createArg(name: String, help: String, builder: AbstractArg<T>.() -> Unit) =
        (object : AbstractArg<T>() {

        }).also {
            it.builder()
            args += it
        }
}

class TestCommandArgs : Arguments() {
    val test by string("yay", "nay")
    val anotherOne by string("huhu", "lol") {
        defaultValue = test
    }
}

abstract class AbstractArg<T> : ReadOnlyProperty<Arguments, T> {
    private var parsed: T? = null
    private var success = false
    var defaultValue: T? = null

    override fun getValue(thisRef: Arguments, property: KProperty<*>): T {
        return if (success) parsed!! else defaultValue!!
    }
}
