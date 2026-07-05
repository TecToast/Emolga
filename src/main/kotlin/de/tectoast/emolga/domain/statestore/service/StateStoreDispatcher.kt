package de.tectoast.emolga.domain.statestore.service

import de.tectoast.emolga.domain.statestore.model.StateStore
import de.tectoast.emolga.domain.statestore.repository.StateStoreRepository
import de.tectoast.emolga.utils.ErrorOrNull
import de.tectoast.emolga.utils.K18n_StateStore
import de.tectoast.emolga.utils.handler.HandlerRegistry
import org.koin.core.annotation.Single

@Single
class StateStoreDispatcher(val repository: StateStoreRepository, handlers: List<StateStoreHandler<StateStore>>) {
    val registry = HandlerRegistry(handlers)

    @Suppress("UNCHECKED_CAST")
    suspend inline fun <T : StateStore, H : StateStoreHandler<T>, R> process(
        state: T,
        user: Long,
        block: context(T) H.() -> R
    ): R {
        val handler = registry.getHandler(state) as H
        return with(state) {
            val result = handler.block()
            afterOperation(user)
            result
        }
    }

    suspend inline fun <reified T : StateStore, H : StateStoreHandler<T>> processIgnoreMissing(
        user: Long,
        block: context(T) H.() -> Unit
    ) =
        (repository.get(user, T::class.simpleName!!) as? T)?.let { process(it, user, block) }

    suspend inline fun <reified T : StateStore, H : StateStoreHandler<T>> process(
        user: Long,
        block: context(T) H.() -> Unit
    ): ErrorOrNull {
        processIgnoreMissing(user, block) ?: return K18n_StateStore.InteractionNotValid
        return null
    }

    suspend fun StateStore.afterOperation(user: Long) {
        if (forDeletion) {
            repository.delete(user, this)
        } else {
            repository.save(user, this)
        }
    }
}