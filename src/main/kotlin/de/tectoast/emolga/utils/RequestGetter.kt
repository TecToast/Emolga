package de.tectoast.emolga.utils

import com.google.api.services.sheets.v4.model.CellData
import com.google.api.services.sheets.v4.model.RowData
import com.google.api.services.sheets.v4.model.Sheet
import de.tectoast.emolga.utils.Google.sheetsService
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

class RequestGetter(private val sid: String) {
    private val ranges: MutableList<String> = LinkedList()
    fun addRange(range: String): RequestGetter {
        ranges.add(range)
        return this
    }

    fun execute(): List<List<List<String>>>? {
        try {
            val sh = sheetsService!!.spreadsheets()[sid].setIncludeGridData(true).setRanges(ranges)
                .execute() //Google.getSheetsService().spreadsheets().values().batchGet("").
            val map = HashMap<String, AtomicInteger>()
            val ret: MutableList<List<List<String>>> = ArrayList(ranges.size)
            for (range in ranges) {
                logger.info("range = {}", range)
                val sheetname = range.split("!".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
                ret.add(
                    sh.sheets.stream().filter { s: Sheet -> s.properties.title == sheetname }
                        .findFirst()
                        .orElse(null).data[map.computeIfAbsent(sheetname) { AtomicInteger(-1) }
                        .incrementAndGet()]
                        .rowData.stream().map { rd: RowData ->
                            rd.getValues().stream().filter { cd: CellData -> cd.effectiveValue != null }
                                .map cd@{ cd: CellData ->
                                    val v = cd.effectiveValue
                                    if (v.stringValue != null) return@cd v.stringValue
                                    val d = v.numberValue
                                    if (d % 1.0 != 0.0) return@cd String.format("%s", d) else return@cd String.format(
                                        "%.0f",
                                        d
                                    )
                                }.collect(Collectors.toList())
                        }.collect(Collectors.toList())
                )
            }
            return ret
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RequestGetter::class.java)
    }
}