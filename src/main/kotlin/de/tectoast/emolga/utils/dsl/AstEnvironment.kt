package de.tectoast.emolga.utils.dsl

import kotlin.reflect.KClass

interface AstEnvironment {
    fun <T : Any> resolve(variable: String, clazz: KClass<T>): T
}

fun AstEnvironment.resolveInt(variable: String): Int = resolve(variable, Int::class)
fun AstEnvironment.resolveString(variable: String): String = resolve(variable, String::class)
fun AstEnvironment.resolveBoolean(variable: String): Boolean = resolve(variable, Boolean::class)