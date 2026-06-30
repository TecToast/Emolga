package de.tectoast.emolga.utils.handler

class HandlerRegistry<C : Any, H : BaseHandler<C>>(handlers: List<H>) {
    private val handlerMap = handlers.associateBy { it.targetClass }

    fun getHandler(config: C): H {
        val klass = config::class
        return handlerMap[klass] ?: error("No handler found for class $klass among handlers ${handlerMap.keys}")
    }
}