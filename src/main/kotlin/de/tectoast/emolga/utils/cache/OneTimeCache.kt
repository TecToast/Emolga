package de.tectoast.emolga.utils.cache

class OneTimeCache<T>(initial: T? = null, private val function: suspend () -> T) : Cache<T>() {
    init {
        cached = initial
    }

    override suspend fun update() = function()
}