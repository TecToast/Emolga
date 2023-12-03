package de.tectoast.emolga.utils.repeat

import kotlinx.coroutines.*
import java.time.Instant
import java.time.temporal.TemporalAmount

class RepeatTask(
    lastExecution: Instant,
    amount: Int,
    difference: TemporalAmount,
    printDelays: Boolean = false,
    consumer: suspend (Int) -> Unit,
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        val now = Instant.now()
        if (lastExecution.isAfter(now)) {
            var last = lastExecution
            var currAmount = amount
            while (!last.isBefore(now)) {
                val finalCurrAmount = currAmount
                val delay = last.toEpochMilli() - now.toEpochMilli()
                if (printDelays) System.out.printf("%d -> %d%n", currAmount, delay)
                scope.launch {
                    delay(delay)
                    consumer(finalCurrAmount)
                }
                currAmount--
                last -= difference
            }
        } else {
            println("LastExecution is in the past, RepeatTask will be terminated")
        }
    }

    fun clear() {
        scope.cancel("Clear called")
    }
}
