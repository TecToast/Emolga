package de.tectoast.emolga.utils.sql.managers

import de.tectoast.emolga.commands.Command.Companion.removeNickFromCache
import de.tectoast.emolga.commands.Command.Companion.toSDName
import de.tectoast.emolga.commands.Command.Translation
import de.tectoast.emolga.utils.sql.base.Condition.and
import de.tectoast.emolga.utils.sql.base.Condition.or
import de.tectoast.emolga.utils.sql.base.DataManager
import de.tectoast.emolga.utils.sql.base.DataManager.ResultsFunction
import de.tectoast.emolga.utils.sql.base.columns.BooleanColumn
import de.tectoast.emolga.utils.sql.base.columns.StringColumn
import java.sql.ResultSet
import java.util.*

object TranslationsManager : DataManager("translations") {
    private val ENGLISHID = StringColumn("englishid", this)
    private val GERMANID = StringColumn("germanid", this)
    private val ENGLISHNAME = StringColumn("englishname", this)
    private val GERMANNAME = StringColumn("germanname", this)
    private val TYPE = StringColumn("type", this)
    private val MODIFICATION = StringColumn("modification", this)
    private val ISNICK = BooleanColumn("isnick", this)
    private val FORME = StringColumn("forme", this)
    private val CAP = BooleanColumn("cap", this)

    init {
        setColumns(ENGLISHID, GERMANID, ENGLISHNAME, GERMANNAME, TYPE, MODIFICATION, ISNICK, FORME, CAP)
    }

    fun getTranslation(id: String?, checkOnlyEnglish: Boolean, withCap: Boolean = false): ResultSet {
        return read(
            selectAll(
                and(
                    or(ENGLISHID.check(id), GERMANID.check(id), !checkOnlyEnglish),
                    CAP.check(withCap)
                )
            ), ResultsFunction { r -> r })
    }

    fun getTranslationList(l: Collection<String?>): ResultSet {
        return read(
            selectAll(
                "(${
                    l.joinToString(" or ") { str: String? ->
                        "englishid=\"" + toSDName(
                            str!!
                        ) + "\""
                    }
                })"
            ), ResultsFunction { r -> r })
    }

    fun addNick(nick: String?, t: Translation) {
        val s = toSDName(nick!!)
        replaceIfExists(s, s, t.otherLang, t.translation, t.type.id, "default", true, null, false)
    }

    fun removeNick(nick: String?): Boolean {
        val sd = toSDName(nick!!)
        removeNickFromCache(sd)
        return delete(and(ENGLISHID.check(sd), ISNICK.check(true))) != 0
    }

    fun getAllMonNicks(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        readWrite(selectAll(and(TYPE.check("pkmn"), ISNICK.check(true))), ResultsFunction { set ->
            forEach(set) { res ->
                map[res.getString("englishid").replaceFirstChar { it.uppercaseChar() }] = res.getString("germanname")
            }
        })
        return map
    }

    fun removeDuplicates() {
        readWrite<Any>(selectAll(TYPE.check("trainer"))) { set: ResultSet ->
            val l: MutableList<String> = LinkedList()
            while (set.next()) {
                val value = ENGLISHID.getValue(set)
                if (l.contains(value)) {
                    set.deleteRow()
                } else {
                    l.add(value)
                }
            }
        }
    }
}