package de.tectoast.emolga.domain.league.core.model

import kotlin.reflect.KClass

abstract class LeagueAttribute<T : Any>(val name: String, val clazz: KClass<T>, val defaultValue: T? = null)