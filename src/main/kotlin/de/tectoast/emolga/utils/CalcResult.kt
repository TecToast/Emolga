@file:OptIn(
    ExperimentalSerializationApi::class,
    ExperimentalTime::class
)

package de.tectoast.emolga.utils

import de.tectoast.emolga.features.interaction.InteractionData
import de.tectoast.emolga.utils.CalcResult.Success
import de.tectoast.k18n.generated.K18nMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.time.ExperimentalTime


sealed interface CalcResult<T> {
    data class Success<T>(val value: T) : CalcResult<T>
    data class Error<T>(val message: K18nMessage) : CalcResult<T>
}

fun <T, R> CalcResult<T>.map(transform: (T) -> R): CalcResult<R> =
    when (this) {
        is Success -> Success(transform(value))
        is CalcResult.Error -> CalcResult.Error(message)
    }


fun <T> T.success() = Success(this)
fun <T> K18nMessage.error() = CalcResult.Error<T>(this)
fun K18nMessageOrError.msg() = when (this) {
    is Success -> value
    is CalcResult.Error -> message
}

inline fun <T, R> CalcResult<T>.getOrReturn(action: (CalcResult.Error<R>) -> Nothing): T =
    when (this) {
        is Success -> value
        is CalcResult.Error -> action(message.error())
    }


@OptIn(ExperimentalContracts::class)
inline fun <T> CalcResult<T>.onFailure(action: (K18nMessage) -> Unit): T? {
    contract {
        returns(null) implies (this@onFailure is CalcResult.Error<T>)
        returnsNotNull() implies (this@onFailure is Success<T>)
    }
    if (this.isError()) {
        action(message)
        return null
    }
    return value
}

@OptIn(ExperimentalContracts::class)
context(iData: InteractionData)
fun <T> CalcResult<T>.onFailureReply(): T? {
    contract {
        returns(null) implies (this@onFailureReply is CalcResult.Error<T>)
        returnsNotNull() implies (this@onFailureReply is Success<T>)
    }
    return onFailure { iData.reply(it, ephemeral = true) }
}

@OptIn(ExperimentalContracts::class)
fun <T> CalcResult<T>.isError(): Boolean {
    contract {
        returns(true) implies (this@isError is CalcResult.Error<T>)
        returns(false) implies (this@isError is Success<T>)
    }
    return this is CalcResult.Error<T>
}

@OptIn(ExperimentalContracts::class)
fun <T> CalcResult<T>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is Success<T>)
        returns(false) implies (this@isSuccess is CalcResult.Error<T>)
    }
    return this is Success<T>
}

fun <T> CalcResult<T>.unwrap(): T {
    return when (this) {
        is Success -> this.value
        is CalcResult.Error -> error("Tried to unwrap an error CalcResult: $message")
    }
}

typealias ErrorOrNull = K18nMessage?
typealias K18nMessageOrError = CalcResult<K18nMessage>