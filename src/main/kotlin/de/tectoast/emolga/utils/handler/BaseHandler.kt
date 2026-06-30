package de.tectoast.emolga.utils.handler

import org.koin.core.component.KoinComponent
import kotlin.reflect.KClass

interface BaseHandler<C : Any> : KoinComponent {
    val targetClass: KClass<C>
}