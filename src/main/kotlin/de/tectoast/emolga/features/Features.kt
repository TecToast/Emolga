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

}

class TestCommandArgs : Arguments() {
    val test by StringArg()
}

abstract class AbstractArg<T> : ReadOnlyProperty<Arguments, T> {
    override fun getValue(thisRef: Arguments, property: KProperty<*>): T {

    }
}

class StringArg : AbstractArg<String>() {


}
