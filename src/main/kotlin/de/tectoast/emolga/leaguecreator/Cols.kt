package de.tectoast.emolga.leaguecreator

import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.xc

enum class Cols(val spread: Cols? = null) {
    LOGO, PLAYER, TEAMNAME, POKEMON, KILLS, DEATHS, DIFF, KILLSPERUSE, USES, POINTS, WINS, LOOSES, GAMES, STRIKES, KILLSSPREAD(
        spread = KILLS
    ),
    ICON;


    operator fun invoke(genData: TableGenData) = ColOnTableData(this, genData)
    operator fun get(row: Int, gamedays: Int, dataSheet: String, team: String? = null) =
        ColOnMonData(this, row, gamedays, dataSheet, team)

    fun forRange(start: Int, end: Int, gamedays: Int) = ColRange(this, start, end, gamedays)

    fun vLookup(monNameCoord: Coord, monCount: Int, dataY: Int, dataMonCount: Int, gamedays: Int, dataSheet: String) =
        ColVLookup(this, monNameCoord, monCount, dataY, dataMonCount, gamedays, dataSheet)
}

class ColRange(val col: Cols, val start: Int, val end: Int, val gamedays: Int) {
    override fun toString(): String {
        val xc = when (col) {
            Cols.KILLS -> {
                (gamedays + 3).xc()
            }

            Cols.DEATHS -> {
                (gamedays * 2 + 5).xc()
            }

            Cols.USES -> {
                (gamedays * 2 + 6).xc()
            }

            Cols.POKEMON -> {
                "B"
            }

            else -> {
                col.spread?.let {
                    val (startX, endX) = when (it) {
                        Cols.KILLS -> {
                            "C" to (gamedays + 2).xc()
                        }

                        else -> error("Column $it as spread of $col is not supported in ColWithRange")
                    }
                    return "={$startX$start:$endX$end}"
                } ?: error("Column $col is not supported in the teamsite with one arg")
            }
        }
        return "={$xc$start:$xc$end}"
    }

    private val onlyRange get() = toString().drop(2).dropLast(1)

    operator fun div(col: Cols) = div(col, 2)

    fun div(col: Cols, decimals: Int = 2) = "=ARRAYFORMULA(WENNFEHLER(RUNDEN(${this.onlyRange} / ${
        col.forRange(
            start, end, gamedays
        ).onlyRange
    };$decimals);0))"

    operator fun minus(col: Cols) = "=ARRAYFORMULA(${this.onlyRange} - ${col.forRange(start, end, gamedays).onlyRange})"
}

class ColOnTableData(private val col: Cols, val data: TableGenData) {
    operator fun plus(c: Cols) = combineWith(c, "+")
    operator fun minus(c: Cols) = combineWith(c, "-")
    operator fun times(c: Cols) = combineWith(c, "*")
    operator fun times(i: Int): String = getBaseFormula(col, false).let { "$it * $i" }

    private fun getBaseFormula(c: Cols, withReplaceEquals: Boolean) =
        (data.tablecols.columnFrom(c).takeUnless { it == "A" }?.let { "=$it${data.tableY}" } ?: data.getFormulaForTable(
            c
        )).let { form ->
            if (withReplaceEquals && form.startsWith("=")) form.substring(1) else form
        }


    private fun combineWith(c: Cols, operator: String): String {
        val base = getBaseFormula(col, false)
        val other = getBaseFormula(c, true)
        return "$base $operator $other"
    }

    operator fun div(c: Cols) = "=WENNFEHLER(RUNDEN(${getBaseFormula(col, true)} / ${getBaseFormula(c, true)};2);0)"
}

class ColOnMonData(
    private val col: Cols,
    val row: Int,
    private val gamedays: Int,
    private val dataSheet: String,
    val team: String? = null
) {
    override fun toString() = if (col == Cols.PLAYER) team!! else "=$dataSheet!${
        (when (col) {
            Cols.KILLS -> "${(gamedays + 3).xc()}$row"
            Cols.DEATHS -> "${(gamedays * 2 + 5).xc()}$row"
            Cols.USES -> "${(gamedays * 2 + 6).xc()}$row"
            Cols.POKEMON -> "B$row"
            else -> error("Column $col is not supported in the teamsite with one arg")
        })
    }"

    operator fun minus(c: Cols) = this.toString() + " - " + c[row, gamedays, dataSheet, team].toString().substring(1)
    operator fun div(c: Cols) =
        "=WENNFEHLER(RUNDEN(" + this.toString().substring(1) + " / " + c[row, gamedays, dataSheet, team].toString()
            .substring(1) + ";2);0)"
}

class ColVLookup(
    private val col: Cols,
    private val monNameCoord: Coord,
    private val monCount: Int,
    private val dataY: Int,
    private val dataMonCount: Int,
    private val gamedays: Int,
    private val dataSheet: String
) {
    override fun toString() =
        "=ARRAYFORMULA(WENNFEHLER(SVERWEIS(${monNameCoord.withoutSheet}:${monNameCoord.plusY(monCount - 1).withoutSheet};${
            when (col) {
                Cols.KILLS -> "$dataSheet!\$B$$dataY:$${(gamedays + 3).xc()}$${dataY + dataMonCount - 1};${gamedays + 2}"
                Cols.DEATHS -> "$dataSheet!\$B$$dataY:$${(gamedays * 2 + 5).xc()}$${dataY + dataMonCount - 1};${gamedays * 2 + 4}"
                Cols.USES -> "$dataSheet!\$B$$dataY:$${(gamedays * 2 + 6).xc()}$${dataY + dataMonCount - 1};${gamedays * 2 + 5}"
                else -> error("Column $col is not supported in the teamsite with one arg")
            }
        };0);0))"

    operator fun minus(c: Cols) = this.toString().dropLast(1) + " - " + c.vLookup(
        monNameCoord, monCount, dataY, dataMonCount, gamedays, dataSheet
    ).toString().substring(14)

    operator fun div(c: Cols) = div(c, 2)

    fun div(c: Cols, decimals: Int = 2) =
        "=WENNFEHLER(RUNDEN(" + this.toString().let { it.substring(1, it.length - 1) } + " / " + c.vLookup(
            monNameCoord, monCount, dataY, dataMonCount, gamedays, dataSheet
        ).toString().let { it.substring(14, it.length - 1) } + "); $decimals); 0)"
}
