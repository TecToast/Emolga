package de.tectoast.emolga.league

import de.tectoast.emolga.utils.Google
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.indexedBy
import de.tectoast.emolga.utils.records.Coord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class DraftOrderCreator private constructor() {
    var indexWrapper: ((Int) -> Coord)? = null
    var rounds = 0
    var draftTableWrapper: ((Int) -> Coord)? = null
    var sid: String? = null
    var playerCount = 0
    var requestBuilder: RequestBuilder? = null
    var fromDoc: Pair<String, List<String>>? = null
    var disabledDoc = false
    fun execute(): Map<Int, List<Int>> {
        val finalOrder = mutableMapOf<Int, List<Int>>()
        val b = requestBuilder ?: RequestBuilder(sid!!)
        fromDoc?.let { (range, list) ->
            val get = Google[sid!!, range, false]
            for (gdi in 0 until rounds) {
                val order = (0 until playerCount).map {
                    get[it][gdi].toString().indexedBy(list)
                        .also { index -> if (index < 0) throw IllegalArgumentException("get[$it][$gdi] (${get[it][gdi]} not found in $list") }
                }
                save(finalOrder = finalOrder, round = gdi + 1, orderForRound = order, b = b)
            }
        } ?: run {
            val table = (0 until playerCount).toList()
            for (i in 1..rounds) {
                save(
                    finalOrder = finalOrder,
                    round = i,
                    orderForRound = if (i % 2 == 0) {
                        finalOrder.getValue(i - 1).reversed()
                    } else {
                        table.shuffled()
                    },
                    b = b
                )
            }
        }
        logger.info("o = {}", Json.encodeToString(finalOrder))
        if (requestBuilder == null)
            b.execute()
        return finalOrder
    }

    private fun save(
        finalOrder: MutableMap<Int, List<Int>>, round: Int, orderForRound: List<Int>, b: RequestBuilder
    ) {
        finalOrder[round] = orderForRound
        if (!disabledDoc) {
            val x = round - 1
            b.addColumn(draftTableWrapper!!(x), orderForRound.map {
                "=" + indexWrapper!!(it)
            }.toList())
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DraftOrderCreator::class.java)

        @JvmStatic
        fun create(builder: DraftOrderCreator.() -> Unit): DraftOrderCreator {
            return DraftOrderCreator().apply(builder)
        }
    }
}
