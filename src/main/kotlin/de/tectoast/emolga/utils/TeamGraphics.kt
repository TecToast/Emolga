package de.tectoast.emolga.utils

import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.only
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.*
import java.awt.image.BufferedImage
import java.util.concurrent.atomic.AtomicInteger
import javax.imageio.ImageIO
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

object TeamGraphics {

    const val OFFSET = 199
    val tierlist = listOf("S", "A", "B", "C", "D")
    val specialColors = listOf(
        "emolga",
        "trapinch",
        "lapras",
        "stantler",
        "wooloo",
        "eevee",
        "spidops",
        "inkay"
    )


    val tiercolors = mapOf(
        "S" to (Color(0x0054D9) to Color(0x42354)),
        "A" to (Color(0xa3008f) to Color(0x54024a)),
        "B" to (Color(0xbd5959) to Color(0x4d2424)),
        "C" to (Color(0xa4c2f4) to Color(0x455266)),
        "D" to (Color(0xea9999) to Color(0x664242)),
    ).withDefault { (Color(0xffd700) to Color(0x423510)) }

    suspend fun fromDraftPokemon(
        mons: List<DraftPokemon>
    ): Pair<BufferedImage, RandomTeamData> = TeamGraphic().create(mons)


}

class TeamGraphic {

    private val currentNumber: MutableMap<Int, Int> = mutableMapOf()
    val mons = mutableListOf<MutableList<DraftPokemon>>()

    //private val shinyCount = AtomicInteger(0)
    private val randomTeamData = RandomTeamData()

    suspend fun create(mons: List<DraftPokemon>): Pair<BufferedImage, RandomTeamData> {
        mons.sortedWith(compareBy({ it.tier.indexedBy(TeamGraphics.tierlist) }, { it.name }))
            .groupBy { it.tier }.forEach {
                addMonsToList(it.value)
            }
        for ((index, monlist) in this.mons.withIndex()) {
            if (index != this.mons.lastIndex) {
                if (monlist.size < 2) monlist.add(this.mons[index + 1].removeFirst())
                while (monlist.size > 4) this.mons[index + 1].add(0, monlist.removeLast())
            }
        }
        return execute().apply {
            this@TeamGraphic.mons.clear()
            currentNumber.clear()
        } to randomTeamData
    }

    private suspend fun addMonsToList(monlist: List<DraftPokemon>) {
        //mons.computeIfAbsent(tier) { ArrayList() } += pokemon
        mons.add(monlist.map {
            DraftPokemon(
                getSpriteForTeamGraphic(it.name, randomTeamData, Constants.G.ASL),
                it.tier
            )
        }
            .toMutableList())
    }

    private suspend fun getSpriteForTeamGraphic(str: String, data: RandomTeamData, guild: Long): String {
        if (str == "Sen-Long") data.hasDrampa = true
        val o = db.getDataObject(str, guild)
        val odds = db.config.only().teamgraphicShinyOdds
        return buildString {
            append("gen5_cropped")
            if (Random.nextInt(odds) == 0) {
                append("_shiny")
                data.shinyCount.incrementAndGet()
            }
            append("/")
            append((o.baseSpecies ?: o.name).toSDName())
            o.forme?.let {
                append("-${it.toSDName()}")
            }
            append(".png")
        }
    }


    private suspend fun execute(): BufferedImage {
        val normal = 220
        var lastNum = normal
        var minOffset = normal
        var maxOffset = normal
        val toexecute = mutableListOf<suspend (Graphics2D, Int) -> Unit>()
        val indexToStartX: MutableMap<Int, Int> = mutableMapOf()
        for ((index, row) in mons.withIndex()) {
            val startX = if (index == 0) normal else {
                val oldsize = mons[index - 1].size
                val csize = row.size
                (lastNum + (if (oldsize > csize) TeamGraphics.OFFSET else if (oldsize < csize) -TeamGraphics.OFFSET else TeamGraphics.OFFSET) + additionalOffset(
                    oldsize, csize
                ) /*- 2 * offset * ((oldsize - csize) / 2)*/)
            }
            lastNum = startX
            minOffset = min(minOffset, startX)
            maxOffset = maxOf(maxOffset, startX + row.size * 397)
            for (mon in row) {
                val suspendLambda: suspend (Graphics2D, Int) -> Unit = { g, i ->
                    executeSlot(g, mon, index, startX + i)
                }
                toexecute.add(suspendLambda)
            }
            indexToStartX[index] = startX
        }
        //logger.info("ja lol ey: $indexToStartX")
        val img = BufferedImage(/*410 * mons.values.maxOf { it.size }*//* maxOffset*/ /*+ offset * mons.keys.size*/
            //mons.maxOf { it.size } * 397 + 42,
            //maxOffset,
            mons.mapIndexed { index, list -> list.size * 397 + indexToStartX[index]!!.coerceAtLeast(20) /* - 210 + 31*/ }
                .max(),
            360 * mons.size + 112,
            BufferedImage.TYPE_INT_ARGB
        )
        //val img = BufferedImage(5000, 5000, BufferedImage.TYPE_INT_ARGB )
        val g = img.createGraphics()
        for (exec in toexecute) {
            exec(g, normal - minOffset)
        }
        g.dispose()
        return img
    }

    private fun additionalOffset(oldsize: Int, csize: Int): Int {
        return when (oldsize.minus(csize)) {
            4 -> 2 * TeamGraphics.OFFSET
            -3 -> -2 * TeamGraphics.OFFSET
            else -> 0
        }
    }

    private suspend fun executeSlot(g: Graphics2D, mon: DraftPokemon, rownum: Int, startX: Int) {
        val (pokemon, tier, _, _) = mon
        g.stroke = BasicStroke(13.5f)
        //g.color = Color(0x00CCCC)
        val x = currentNumber.getOrPut(rownum) { 0 }
        currentNumber[rownum] = currentNumber[rownum]!! + 1
        val xcoord = x.y(20 + 377, startX)
        val ycoord = rownum.y(360, 220)
        val ora = withContext(Dispatchers.IO) {
            ImageIO.read(pokemon.file()/*.also { logger.info("Mon: ${it.absolutePath}") }*/)
        }
        val factor = 330f / 96f
        val width = (ora.width.toFloat() * factor).toInt()
        val height = (ora.height.toFloat() * factor).toInt()
        val cyanHexagon = hexagon(xcoord, ycoord, 210)
        //val scaledOra = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        g.clip = cyanHexagon/*
        graphics.drawImage(ora, 0, 0, width, height, null)
        graphics.composite = AlphaComposite.getInstance(AlphaComposite.CLEAR)
        graphics.color = Color(255, 255, 255, 255)
        for (xc in xcoord - width / 2..xcoord + width / 2) {
            for (yc in ycoord - height / 2..ycoord + height / 2) {
                if (!cyanHexagon.contains(xc, yc)) {
                    graphics.drawRect(xc, yc, 1, 1)
                }
            }
        }
        graphics.dispose()*/
        g.drawImage(
            ora, xcoord - width / 2, ycoord - height / 2, width, height, null
        )/*g.color = Color.RED
        g.drawRect(xcoord - 165, ycoord - 150, 1, 1)*/
        g.clip = null
        g.color = Color.WHITE
        //g.paint = GradientPaint(xcoord.toFloat() - 200, ycoord.toFloat() + 200, Color.WHITE, xcoord.toFloat() + 200, ycoord.toFloat() - 200, Color.BLACK)
        g.drawPolygon(hexagon(xcoord, ycoord, 200))
        //g.color = Color(if (pokemon == "emolga") 0xFFD700 else 0x009999)
        val extend = 0
        val isSpecial = pokemon.substringAfterLast("/").substringBefore(".") in TeamGraphics.specialColors
        g.paint = GradientPaint(
            xcoord.toFloat() - 220 - extend,
            ycoord.toFloat() + 220 + extend,
            Color(if (isSpecial) 0xffd700 else 0x8675b8),
            xcoord.toFloat() + 220 + extend,
            ycoord.toFloat() - 220 - extend,
            Color(if (isSpecial) 0x423510 else 0x473c61)
        )
        g.drawPolygon(cyanHexagon)
        g.color = Color.BLACK
        g.fillPolygon(hexagon(xcoord, ycoord + 200, 40))
        g.color = Color.WHITE
        g.stroke = BasicStroke(8.5f)
        g.drawPolygon(hexagon(xcoord, ycoord + 200, 40))
        g.stroke = BasicStroke(8f)
        //g.color = tiercolors[tier]
        val trimmedTier = tier.take(1)
        TeamGraphics.tiercolors.getValue(trimmedTier).let {
            g.paint = GradientPaint(
                xcoord.toFloat(),
                (ycoord + 245).toFloat(),
                it.first,
                xcoord.toFloat(),
                (ycoord + 155).toFloat(),
                it.second
            )
        }
        //g.color = Color(0x8e7cc3)
        g.drawPolygon(hexagon(xcoord, ycoord + 200, 45))
        g.color = Color.WHITE
        g.font = Font(Font.DIALOG, Font.PLAIN, 40)
        g.drawString(
            trimmedTier, xcoord - when (trimmedTier) {
                "S" -> 12
                "A" -> 12
                "B" -> 14
                "C" -> 15
                "M" -> 18
                else -> 15
            }, ycoord + 215
        )


    }

    private fun hexagon(midX: Int, midY: Int, size: Int): Polygon = Polygon().apply {
        for (i in 0..5) addPoint(
            (midX + size * sin(i * 2 * Math.PI / 6)).toInt(),
            (midY + size * cos(i * 2 * Math.PI / 6)).toInt(),
        )
    }
}

data class RandomTeamData(val shinyCount: AtomicInteger = AtomicInteger(), var hasDrampa: Boolean = false)
