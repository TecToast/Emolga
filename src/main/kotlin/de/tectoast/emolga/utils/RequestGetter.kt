package de.tectoast.emolga.utils

import com.google.api.services.sheets.v4.model.CellData
import de.tectoast.emolga.utils.Google.sheetsService
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@Suppress("unused")
class RequestGetter(private val sid: String) {
    private val ranges: MutableList<String> = LinkedList()
    fun addRange(range: String): RequestGetter {
        ranges.add(range)
        return this
    }

    fun execute(): List<List<List<String>>> {
        val sh = sheetsService!!.spreadsheets()[sid].setIncludeGridData(true).setRanges(ranges)
            .execute() //Google.getSheetsService().spreadsheets().values().batchGet("").
        val map = HashMap<String, AtomicInteger>()
        val ret: MutableList<List<List<String>>> = ArrayList(ranges.size)
        for (range in ranges) {
            logger.info("range = {}", range)
            val sheetname = range.split("!").dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            ret.add(
                sh.sheets.first { it.properties.title == sheetname }.data[map.computeIfAbsent(sheetname) {
                    AtomicInteger(
                        -1
                    )
                }
                    .incrementAndGet()]
                    .rowData.map {
                        it.getValues().filter { cd: CellData -> cd.effectiveValue != null }
                            .map cd@{ cd: CellData ->
                                val v = cd.effectiveValue
                                if (v.stringValue != null) return@cd v.stringValue
                                val d = v.numberValue
                                if (d % 1.0 != 0.0) return@cd String.format("%s", d) else return@cd String.format(
                                    "%.0f",
                                    d
                                )
                            }
                    }
            )
        }
        return ret

    }

    companion object {
        private val logger = LoggerFactory.getLogger(RequestGetter::class.java)
    }
}