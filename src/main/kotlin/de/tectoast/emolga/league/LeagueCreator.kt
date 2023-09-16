package de.tectoast.emolga.league

import com.google.api.services.sheets.v4.model.CellFormat
import com.google.api.services.sheets.v4.model.Color
import com.google.api.services.sheets.v4.model.TextFormat
import de.tectoast.emolga.commands.*
import de.tectoast.emolga.commands.Command.Companion.convertColor
import de.tectoast.emolga.commands.Command.Companion.getAsXCoord
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.league.Cols.*
import de.tectoast.emolga.utils.RequestBuilder
import de.tectoast.emolga.utils.draft.DraftPokemon
import de.tectoast.emolga.utils.draft.Tierlist
import de.tectoast.emolga.utils.json.SignUpData
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.draft.AllowedData
import de.tectoast.emolga.utils.json.emolga.draft.Default
import de.tectoast.emolga.utils.json.emolga.draft.League
import de.tectoast.emolga.utils.json.get
import de.tectoast.emolga.utils.json.showdown.Pokemon
import de.tectoast.emolga.utils.records.Coord
import de.tectoast.emolga.utils.records.TableCoord
import dev.minn.jda.ktx.coroutines.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.JDA
import org.bson.BsonObjectId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.litote.kmongo.contains
import java.security.SecureRandom
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import de.tectoast.emolga.league.DraftOrderCreator.Companion.fromLeagueCreator as fromLeagueCreatorDraft
import de.tectoast.emolga.league.GamePlanCreator.Companion.fromLeagueCreator as fromLeagueCreatorGamePlan
import de.tectoast.testingarea.utils.Templater.ShowdownScriptTemplate.Format as ScriptFormat


class LeagueCreator(val name: String) {

    val filename by lazy { "$name.json" }
    private var copyonly = false

    var monCount: Int by Delegates.notNull()
    var monsSupplier: (suspend (Int) -> List<String>)? = null
    var playerCount: Int by Delegates.notNull()
    lateinit var playerNameIndexer: ((Int) -> Coord)
    var playerTeamIndexer: ((Int) -> Coord)? = null
    var logoIndexer: ((Int) -> Coord)? = null
    var sdnameIndexer: ((Int) -> Coord)? = null
    lateinit var tablecols: List<Cols>
    var gamedays: Int by Delegates.notNull()
    var teamsitecols: List<Cols> = listOf()
    var teamsiteDataCoord: ((Int) -> Coord)? = null

    //var teamsiteDataSpecialCoords: Map<Int, Cols> = mutableMapOf()
    var teamsiteMonCoord: ((Int) -> Coord)? = null
    var additionalStuff: (suspend RequestBuilder.() -> Unit)? = null

    var sid: String
        get() = sids.first()
        set(value) {
            sids.add(value)
        }
    val sids = mutableListOf<String>()
    var dataSheet: String = "Data"
    lateinit var tableBase: TableCoord
    internal var tableStep = 1
    var slots = 0
    var slotsOnTeamsite = 0
    internal var addons: Addons? = null

    var teamsiteVLookup: Boolean = true


    val dataMonCount get() = monCount + slots
    val gap: Int get() = dataMonCount + 3
    lateinit var saveJSON: String

    var dataFiller: String? = null

    var guild by Delegates.notNull<Long>()

    var beforeAnything: (RequestBuilder.() -> Unit)? = null

    // true if users in conferences should be randomized
    var jda: (Pair<JDA, Boolean>)? = null

    val onlyExecuteAddons = mutableListOf<KClass<out Addons.Addon>>()
    val disabledLeagueIndexes = mutableSetOf<Int>()

    val tierlist by lazy {
        transaction {
            Tierlist.select { Tierlist.guild eq guild }.map { DraftPokemon(it[Tierlist.pokemon], it[Tierlist.tier]) }
        }
    }

    val collection by lazy {
//        db.db.createCollection("league$name", CreateCollectionOptions().)
        db.db.getCollection<League>("league$name")
    }

    suspend inline fun <reified T> String.put(key: String, value: T) {
        collection.updateOne("{_id: ObjectId('$this')}", "{\$set:{\"$key\":${Json.encodeToString(value)}}}")
        League::table contains 5
//        collection.updateOne("", set(League::leaguename setTo ""))
    }

    suspend fun String.get(): League {
        return collection.findOne("{_id: ObjectId('$this')}")!!
    }

    suspend fun execute(execute: Boolean = true) {
        var names: Map<Long, String>? = null
        var usersByConf: List<List<MutableMap.MutableEntry<Long, SignUpData>>>? = null
        var randomize: Boolean? = null
        jda?.let { j ->
            val jd = j.first
            randomize = j.second
            val signData = db.signups.get(guild)!!
            usersByConf =
                if (signData.conferences.isEmpty()) signData.users.entries.toList().l else signData.users.entries.groupBy { it.value.conference!! }.entries.toList()
                    .sortedBy { signData.conferences.indexOf(it.key) }.map { it.value }
            names = jd.getGuildById(guild)!!.retrieveMembersByIds(usersByConf!!.flatten()
                .map { it.key } + signData.users.values.flatMap { it.teammates }).await()
                .associate { it.idLong to it.effectiveName }

        }
        val saveJSONList = collection.find("{}").toList().map { it.id!!.toString() }.toMutableList()

        sids/*.take(1)*/.forEachIndexed { leagueindex, sid ->
            if (leagueindex in disabledLeagueIndexes) return@forEachIndexed
            val b = RequestBuilder(sid)
            saveJSON = saveJSONList.getOrNull(leagueindex) ?: run {
                (collection.insertOne(Default()).insertedId!! as BsonObjectId).value.toString()
            }
            if (copyonly) {
                val realCollection = db.drafts
                realCollection.insertOne(collection.findOne("{_id: ObjectId('$saveJSON')}")!!)
                return@forEachIndexed
            }

            names?.let { n ->
                val users =
                    usersByConf!![leagueindex].let { if (randomize!!) it.shuffled(SecureRandom()) else it }
                saveJSON.put("table", users.map { it.key })
                val allowed: MutableMap<Long, MutableSet<AllowedData>> = mutableMapOf()
                for (i in 0..<playerCount) {
                    val uid = users[i].key
                    val data = users[i].value
                    if (data.teammates.isNotEmpty()) {
                        allowed[uid] = mutableSetOf(
                            AllowedData(uid, true), *data.teammates.map { AllowedData(it, true) }.toTypedArray()
                        )
                    }
                    b.addSingle(playerNameIndexer(i),
                        listOf(uid, *data.teammates.toTypedArray()).joinToString(" & ") { n[it]!! })
                    playerTeamIndexer?.let {
                        b.addSingle(it(i), data.teamname)
                    }
                    logoIndexer?.let {
                        b.addSingle(it(i), "=IMAGE(\"${data.logoUrl}\";1)")
                    }
                    sdnameIndexer?.let {
                        b.addSingle(it(i), data.sdname)
                    }
                }
                saveJSON.put("allowed", allowed)
                b.execute(execute)
                return@forEachIndexed
            }
            if (onlyExecuteAddons.isNotEmpty()) {
                executeAddons(b, saveJSON)
                b.execute(execute)
                return@forEachIndexed
            }
            saveJSON.put("sid", sid)
            saveJSON.put("guild", guild)
            saveJSON.put("leaguename", if (sids.size == 1) name else "${name}L${leagueindex + 1}")
            beforeAnything?.let {
                b.it()
                b.execute(execute)
                b.clear()
            }
            List(gamedays) { "${it + 1}" }.toTypedArray().let {
                b.addRow("$dataSheet!B1", listOf("Pokemänner", *it, "Kills", "", *it, "Deaths", "Uses"))
            }
            for (user in 0..<playerCount) {
                val y = user.y(gap, 3)
                val endY = y + dataMonCount - 1
                val tableY = user.y(gap, dataMonCount + 4)
                val winLooseY = tableY - 1
                monsSupplier?.let {
                    b.addColumn("$dataSheet!B$y", it(user))
                }
                dataFiller?.let {
                    val fillerList = fill(gamedays, dataMonCount, it)
                    b.addAll("$dataSheet!C$y", fillerList)
                    b.addAll("$dataSheet!${getAsXCoord(gamedays + 5)}$y", fillerList)
                }
                val killCoord = getAsXCoord(gamedays + 3)
                val deathCoord = getAsXCoord(gamedays * 2 + 5)
                val usesCoord = getAsXCoord(gamedays * 2 + 6)
                b.addColumn("$dataSheet!$killCoord$y",
                    List(dataMonCount) { "=SUMME(C${y + it}:${getAsXCoord(gamedays + 2)}${y + it})" })
                b.addColumn("$dataSheet!$deathCoord$y",
                    List(dataMonCount) { "=SUMME(${getAsXCoord(gamedays + 5)}${y + it}:${getAsXCoord(gamedays * 2 + 4)}${y + it})" })
                b.addColumn("$dataSheet!$usesCoord$y",
                    List(dataMonCount) { "=ZÄHLENWENN(${getAsXCoord(gamedays + 5)}${y + it}:${getAsXCoord(gamedays * 2 + 4)}${y + it}; \">-1\")" })
                val gendata =
                    TableGenData(user, killCoord, y, endY, deathCoord, usesCoord, tableY, winLooseY, tablecols, this)
                b.addRow("$dataSheet!B$tableY", tablecols.map {
                    gendata.getFormulaForTable(it)
                })
                b.addSingle(
                    tableBase.provideCoord(user, this).toString(),
                    "={$dataSheet!\$B$$tableY:$${getAsXCoord(tablecols.size + 1)}$$tableY}"
                )
                teamsiteDataCoord?.let {
                    fun doIt(index: Int, col: Cols) {
                        val coord = it(user)
                        if (teamsiteVLookup) {
                            val tsMonCoord = teamsiteMonCoord!!(user)
                            val vLookupKills = KILLS.vLookup(
                                tsMonCoord, monCount + slotsOnTeamsite, y, dataMonCount, gamedays, dataSheet
                            )
                            col.spread?.let {
                                val start = when (it) {
                                    KILLS -> 3
                                    DEATHS -> gamedays + 5
                                    else -> error("INVALID SPREAD $it")
                                }
                                b.addRow("${coord.sheet}!${(coord.x + index).xc()}${coord.y}", List(gamedays) { gdi ->
                                    "=ARRAYFORMULA(WENNFEHLER(SVERWEIS(${tsMonCoord.withoutSheet}:${
                                        tsMonCoord.plusY(
                                            monCount - 1 + slotsOnTeamsite
                                        ).withoutSheet
                                    };$dataSheet!\$B$${user.y(gap, 3)}:\$${gdi.x(1, start)}\$${
                                        user.y(
                                            gap, dataMonCount + 1
                                        )
                                    };${gdi + start - 1};0)))"
                                })
                            } ?: run {
                                b.addSingle(
                                    "${coord.sheet}!${(coord.x + index).xc()}${coord.y}", when (col) {
                                        KILLSPERUSE -> {
                                            vLookupKills / USES
                                        }

                                        DIFF -> vLookupKills - DEATHS
                                        else -> col.vLookup(
                                            tsMonCoord, monCount + slotsOnTeamsite, y, dataMonCount, gamedays, dataSheet
                                        ).toString()
                                    }
                                )
                            }
                        } else {
                            b.addColumn("${coord.sheet}!${(coord.x + index).xc()}${coord.y}", List(monCount) {
                                val row = y + it
                                when (col) {
                                    KILLS, DEATHS, USES -> col[row, gamedays, dataSheet].toString()
                                    KILLSPERUSE -> KILLS[row, gamedays, dataSheet] / USES
                                    DIFF -> KILLS[row, gamedays, dataSheet] - DEATHS
                                    else -> error("Invalid column at teamsite $col")
                                }
                            })
                        }
                    }

                    var spreaded = 0
                    teamsitecols.forEachIndexed { index, col ->
                        doIt(index + spreaded, col)
                        if (col.spread != null) spreaded += gamedays - 1
                    }/*teamsiteDataSpecialCoords.forEach { (index, col) ->
                        doIt(index, col)
                        if (col.spread != null) spreaded += gamedays - 1
                    }*/
                }

            }
            executeAddons(b, saveJSON)
            additionalStuff?.let {
                it(b)
            }
            b.execute(execute)
        }
    }

    fun addons(builder: Addons.() -> Unit) {
        addons = Addons(this).apply(builder)
    }

    private suspend fun executeAddons(b: RequestBuilder, json: String) {
        addons?.run {
            executeAll(b, json)
        }
    }

    fun copyOnly() {
        copyonly = true
    }

    companion object {
        fun create(name: String, builder: LeagueCreator.() -> Unit) = LeagueCreator(name).apply(builder)
    }
}

data class TableGenData(
    val user: Int,
    val killCoord: String,
    val y: Int,
    val endY: Int,
    val deathCoord: String,
    val usesCoord: String,
    val tableY: Int,
    val winLooseY: Int,
    val tablecols: List<Cols>,
    val lc: LeagueCreator
) {
    fun getFormulaForTable(it: Cols) = with(lc) {
        when (it) {
            LOGO -> logoIndexer!!(user).formula
            PLAYER -> playerNameIndexer(user).formula
            TEAMNAME -> playerTeamIndexer!!(user).formula
            POKEMON -> error("Pokemon is not allowed in the table")
            KILLS -> "=SUMME($killCoord$y:$killCoord$endY)"
            DEATHS -> "=SUMME($deathCoord$y:$deathCoord$endY)"
            USES -> "=SUMME($usesCoord$y:$usesCoord$endY)"
            DIFF -> KILLS(this@TableGenData) - DEATHS
            WINS -> "=SUMME(C$winLooseY:${getAsXCoord(gamedays + 2)}$winLooseY)"
            LOOSES -> "=SUMME(${getAsXCoord(gamedays + 5)}$winLooseY:${getAsXCoord(gamedays * 2 + 4)}$winLooseY)"
            GAMES -> WINS(this@TableGenData) + LOOSES
            POINTS -> WINS(this@TableGenData) * 3
            STRIKES -> "0"
            else -> error("INVALID COL $it")
        }
    }
}

@Suppress("FunctionName")
class Addons(val leagueCreator: LeagueCreator) {
    lateinit var b: RequestBuilder
    private val addonList: MutableList<Addon> = mutableListOf()
    val disabledAddons = mutableSetOf<KClass<out Addon>>()

    val vlookUpSize: Int by lazy {
        leagueCreator.tierlist.size
    }

    private inline fun <reified T : Addon> getAddon(): T {
        return addonList.filterIsInstance<T>().first()
    }

    companion object {
        val allAddons by lazy {
            val list = listOf(
                KillList::class,
                GamePlan::class,
                DraftOrder::class,
                KDOnTeamSite::class,
                MatchUps::class,
                PokemonData::class,
                TierlistConditionalFormatting::class,
                ShowdownScript::class
            )
            require(list.containsAll(Addon::class.sealedSubclasses))
            list
        }
    }

    var gameplanIndexes: Map<Int, List<List<Int>>>? = null
    suspend fun LeagueCreator.executeAll(b: RequestBuilder, json: String) {
        json.get().battleorder.takeIf { it.isNotEmpty() }?.let {
            gameplanIndexes = it
        }

        addonList.filter {
            (onlyExecuteAddons.isEmpty() || onlyExecuteAddons.contains(it::class)) && it::class !in disabledAddons
        }.sortedBy { allAddons.indexOf(it::class) }.forEach {
            with(it) {
                println("Executing $this")
                execute(b)
            }
        }
    }

    sealed interface Addon {
        suspend fun LeagueCreator.execute(b: RequestBuilder)
    }

    inner class KillList(builder: KillList.() -> Unit) : Addon {
        var killlistDataLocation: Coord? = null
        lateinit var killlistcols: List<Cols>
        var killlistLocation: String? = null
        lateinit var killlistsort: List<Cols>

        init {
            builder()
            addonList += this
        }

        private val ascendingCols = setOf(DEATHS, USES)
        private val vLookupWholeRangeCols = setOf(ICON)
        override suspend fun LeagueCreator.execute(b: RequestBuilder) {
            val locbase = killlistDataLocation!!
            val vLookUpWholeRange = mutableMapOf<Cols, Int>()
            for (user in 0..<playerCount) {
                val y = user.y(gap, 3)
                val userMappings = mapOf(PLAYER to lazy { playerNameIndexer(user).formula },
                    TEAMNAME to lazy { playerTeamIndexer!!(user).formula },
                    LOGO to lazy { logoIndexer!!(user).formula })
                val end = y + dataMonCount - 1
                killlistcols.forEachIndexed { index, col ->
                    val range = locbase.sheet + "!${(locbase.x + index).xc()}${locbase.y + user * dataMonCount}"
                    when (col) {
                        in userMappings -> {
                            b.addColumn(range, List(dataMonCount) { userMappings[col]!!.value })
                        }

                        in vLookupWholeRangeCols -> vLookUpWholeRange[col] = index
                        else -> {
                            b.addSingle(range, when (col) {
                                POKEMON, KILLS, DEATHS, USES, KILLSSPREAD -> col.forRange(y, end, gamedays).toString()

                                KILLSPERUSE -> KILLS.forRange(y, end, gamedays) / USES
                                DIFF -> KILLS.forRange(y, end, gamedays) - DEATHS

                                else -> {
                                    error("Invalid column at killlist $col")
                                }
                            }.also { println(col.name + " " + it) })
                        }
                    }
                }
            }

            val fullEnd = locbase.y + playerCount * gap - 1
            val pokemonCol = (locbase.x + killlistcols.indexOf(POKEMON)).xc()
            vLookUpWholeRange.forEach { (col, index) ->
                val range = locbase.sheet + "!${(locbase.x + index).xc()}${locbase.y}"
                when (col) {
                    ICON -> b.addSingle(range, getAddon<PokemonData>().run {
                        val datacol =
                            DataCol.ICON.takeIf { it in datacols } ?: DataCol.GEN5SPRITE.takeIf { it in datacols }
                            ?: error("No icon column found in pokemon data")
                        with(datacol) {
                            getFormula("$pokemonCol${locbase.y}:$pokemonCol$fullEnd")
                        }
                    })

                    else -> error("Implementation for vLookUpWholeRange for $col missing")
                }
            }

            killlistLocation?.let { kloc ->
                val y = dataMonCount * playerCount

                b.addSingle(kloc,
                    "=WENNFEHLER(SORT(FILTER(${
                        locbase.spreadTo(
                            killlistcols.size,
                            y
                        )
                    };${locbase.plusX(killlistcols.indexOf(POKEMON).coerceAtLeast(0)).spreadTo(y = y)} <> \"\"); ${
                        killlistsort.joinToString("; ") {
                            "${it.indexedBy(killlistcols) + 1}; ${if (it in ascendingCols) 1 else 0}"
                        }
                    }))")

            }


        }
    }

    inner class TierlistConditionalFormatting(builder: TierlistConditionalFormatting.() -> Unit) : Addon {
        lateinit var range: String
        var sheetidGerman: Int
            get() = sheetidsGerman.first()
            set(value) {
                sheetidsGerman.add(value)
            }
        val sheetidsGerman = mutableSetOf<Int>()
        var sheetidEnglish: Int
            get() = sheetidsEnglish.first()
            set(value) {
                sheetidsEnglish.add(value)
            }
        val sheetidsEnglish = mutableSetOf<Int>()
        var complexBanLimiter: String? = null
        var customSearchRange: String? = null
        var excludeComplexAsBegin = false
        lateinit var format: CellFormat

        init {
            builder()
            addonList += this
        }

        // =ZÄHLENWENN(FILTER(INDIREKT("Data1!B3:B184");INDIREKT("Data1!K3:K184") = "X");C8)
        // =ZÄHLENWENN(FILTER(INDIREKT("Data1!B3:B184");INDIREKT("Data1!K3:K184") = "X");GLÄTTEN(WENNFEHLER(LINKS(C8;FINDEN("(";C8) - 1);C8)))
        override suspend fun LeagueCreator.execute(b: RequestBuilder) {
            val endY = (playerCount - 1).y(gap, gap)
            val stateCol = (gamedays + 4).xc()
            val firstCoord = range.substringBefore(":")
            val searchCoord = complexBanLimiter?.let {
                "GLÄTTEN(WENNFEHLER(LINKS($firstCoord;FINDEN(\"$it\";$firstCoord) - 1);$firstCoord))"
            } ?: firstCoord
            val searchRange = customSearchRange
                ?: """FILTER(INDIREKT("$dataSheet!B3:B$endY");INDIREKT("$dataSheet!${stateCol}3:${stateCol}$endY") = "X")"""
            val germanFormula by lazy {
                "=ZÄHLENWENN($searchRange;$searchCoord)"
            }
            val englishFormula by lazy {
                // =ZÄHLENWENN(INDIREKT("Data!I600:I700"); A5)
                // =ARRAYFORMULA(WENNFEHLER(SVERWEIS(INDIREKT("Data!H600:H700");INDIREKT("Data!K600:N1298");4)))
                // =ZÄHLENWENN(ARRAYFORMULA(WENNFEHLER(SVERWEIS(INDIREKT("Data!H600:H700");INDIREKT("Data!K600:N1298");4))); A5)
                // =ZÄHLENWENN(ARRAYFORMULA(WENNFEHLER(SVERWEIS(FILTER(INDIREKT("Data1!B3:B184");INDIREKT("Data1!K3:K184") = "X");INDIREKT("Data!K600:N1298");4))); A5)
                // =ZÄHLENWENN(ARRAYFORMULA(WENNFEHLER(SVERWEIS(FILTER(INDIREKT("Data!B3:B265");INDIREKT("Data!N3:N265") = "X");INDIREKT("Data!K600:N1298");4))); A5)

                // =ZÄHLENWENN(ARRAYFORMULA(WENNFEHLER(SVERWEIS(FILTER(INDIREKT("Data!B3:B265");INDIREKT("Data!N3:N265") = "X");INDIREKT("Data!K600:N1298");4))); A5)
                val data = addonList.filterIsInstance<PokemonData>().firstOrNull()
                    ?: error("No PokemonData found, but needed for english formula")
                "=ZÄHLENWENN(ARRAYFORMULA(WENNFEHLER(${
                    data.getDefaultVLookup(
                        searchRange, DataCol.ENGLISHTLNAME, indirect = true
                    )
                })); $searchCoord)"
                //"=ZÄHLENWENN(FILTER(INDIREKT(\"Data1!B3:B184\");INDIREKT(\"Data1!K3:K184\") = \"X\");$searchCoord)"
            }
            sheetidsGerman.forEach { id ->
                val form = if (excludeComplexAsBegin) """=WENN(LINKS($firstCoord)="${complexBanLimiter!!}";0;${
                    germanFormula.substring(1)
                })""" else germanFormula
                b.addConditionalFormatCustomFormula(
                    RequestBuilder.ConditionalFormat(form, format), range, id
                )
            }
            sheetidsEnglish.forEach { id ->
//                b.addSingle("Data!J580", englishFormula)
                val form = if (excludeComplexAsBegin) """=WENN(LINKS($firstCoord)="${complexBanLimiter!!}";0;${
                    englishFormula.substring(1)
                })""" else englishFormula
                b.addConditionalFormatCustomFormula(
                    RequestBuilder.ConditionalFormat(englishFormula, format), range, id
                )
            }
        }

    }

    inner class PokemonData(builder: PokemonData.() -> Unit) : Addon {
        lateinit var datacoord: Coord
        lateinit var datacols: List<DataCol>

        // STRING HERE IS JUST THE X COORDINATE
        var oldTeamsiteDataUses: Map<(Int) -> String, DocUsedCols>? = null
        var newTeamsiteDataUses: Map<Int, DocUsedCols>? = null
        val dataProviders: MutableMap<DataCol, String> = mutableMapOf()
        var buffer = 10
        val additionalDataUses: MutableMap<String, Pair<DocUsedCols, String>> = mutableMapOf()

        private var disabled = false
        private val additionalDataProducers = mutableListOf<Runnable>()

        init {
            builder()
            addonList += this
        }

        fun disableGeneration() {
            disabled = true
        }


        private val vLookupCache: MutableMap<Pair<String, DataCol>, String> = mutableMapOf()
        internal fun getDefaultVLookup(area: String, col: DataCol, indirect: Boolean = false): String {
            val pair = area to col
            vLookupCache[pair]?.let { return it }
            val s = "SVERWEIS(${area};${
                dataProviders[col] ?: (datacoord.run {
                    val str = "$sheet!$$xAsC$$y:$${(x + datacols.size).xc()}$${y + vlookUpSize - 1 + buffer}"
                    if (indirect) "INDIREKT(\"$str\")" else str
                } + ";${datacols.indexOf(col) + 2}")
            };0)"
            vLookupCache[pair] = s
            return s
        }


        inner class TLBuilder : Runnable {
            lateinit var sheet: String
            var factor: Int by Delegates.notNull()
            var startXMons: Int by Delegates.notNull()
            var startY: Int by Delegates.notNull()
            var endY: Int by Delegates.notNull()
            var columns: Int by Delegates.notNull()

            val dataMap = mutableMapOf<Int, DataCol>()
            override fun run() {
                for (i in 0 until columns) {
                    val x = i.x(factor, startXMons)
                    dataMap.forEach { (startXData, dataCol) ->
                        additionalDataUses["$sheet!${i.x(factor, startXData)}$startY"] =
                            dataCol to "${x}$startY:${x}$endY"
                    }
                }
            }


        }

        inner class DraftSheetInfo : Runnable {
            lateinit var indexer: (Int) -> Coord
            val cols: MutableList<DocUsedCols> = mutableListOf()

            fun points(tierToPointArea: String) {
                cols += CalcedCol.Points(tierToPointArea)
            }

            override fun run() {
                with(leagueCreator) {
                    for (i in 0 until playerCount) {
                        val range = indexer(i)
                        val area = "$range:${range.plusY(monCount - 1).withoutSheet}"
                        val monCoord = teamsiteMonCoord!!(i)
                        b.addSingle(range.toString(), "={${monCoord.spreadBy(y = monCount - 1)}}")
                        cols.forEachIndexed { index, cols ->
                            additionalDataUses[range.plusX(index + 1).toString()] = cols to area
                        }
                    }
                }
            }
        }

        fun tierlist(builder: TLBuilder.() -> Unit) {
            additionalDataProducers += TLBuilder().apply(builder)
        }

        fun draftSheetInfo(builder: DraftSheetInfo.() -> Unit) {
            additionalDataProducers += DraftSheetInfo().apply(builder)
        }


        override suspend fun LeagueCreator.execute(b: RequestBuilder) {
            this@Addons.b = b
            newSuspendedTransaction {
                val send = mutableListOf<List<Any>>()
                if (/*!dataProviders.keys.containsAll(teamsiteDataUses?.values.orEmpty()) && */!disabled) {
                    val pokedex = Command.dataJSON
                    tierlist.forEach {
                        val tlName = it.name
                        //println(tlName)
                        val translation = NameConventionsDB.getDiscordTranslation(
                            tlName, guild, english = true
                        )!!

                        val o = pokedex[translation.official.toSDName()]!!
                        send += listOf(
                            tlName, *datacols.map { c ->
                                when (c) {
                                    DataCol.TIER -> it.tier
                                    DataCol.ENGLISHTLNAME -> translation.tlName
                                    else -> c.getData(o)
                                }
                            }.toTypedArray()
                        )
                    }
                    b.addAll(datacoord.toString(), send)
                }
            }
            oldTeamsiteDataUses?.let {
                repeat(playerCount) { player ->
                    val monCoord = teamsiteMonCoord!!(player)
                    it.forEach { (coord, col) ->
                        additionalDataUses[monCoord.setX(coord(player)).toString()] =
                            col to "${monCoord.withoutSheet}:${monCoord.plusY(monCount - 1 + slotsOnTeamsite).withoutSheet}"
                    }
                }
            }
            newTeamsiteDataUses?.let {
                repeat(playerCount) { player ->
                    val monCoord = teamsiteMonCoord!!(player)
                    it.forEach { (add, col) ->
                        additionalDataUses[monCoord.setX(monCoord.x + add).toString()] =
                            col to "${monCoord.withoutSheet}:${monCoord.plusY(monCount - 1 + slotsOnTeamsite).withoutSheet}"
                    }
                }
            }
//            teamsiteDataUses?.let {
//                repeat(playerCount) { player ->
//                    val monCoord = teamsiteMonCoord!!(player)
//                    it.forEach { (coord, col) ->
//                        b.addSingle(monCoord.setX(coord(player)).toString(),
//                            "=ARRAYFORMULA(WENNFEHLER(SVERWEIS(${monCoord.withoutSheet}:${monCoord.plusY(monCount - 1 + slotsOnTeamsite).withoutSheet};${
//                                dataProviders[col] ?: (datacoord.run {
//                                    "$sheet!$$xAsC$$y:$${(x + datacols.size).xc()}$${y + vlookUpSize - 1 + buffer}"
//                                } + ";${datacols.indexOf(col) + 2}")
//                            };0)))")
//                    }
//                }
//            }
            additionalDataProducers.forEach { it.run() }
            additionalDataUses.forEach { (t, u) ->
                with(u.first) {
                    b.addSingle(t, getFormula(u.second))
                }
            }
        }

    }

    fun LeagueCreator.GamePlan(builder: GamePlanCreator.() -> Unit) {
        GamePlan(fromLeagueCreatorGamePlan(builder))
    }

    inner class GamePlan(private val builder: GamePlanCreator) : Addon {
        init {
            addonList += this
        }

        override suspend fun LeagueCreator.execute(b: RequestBuilder) {
            builder.requestBuilder = b
            gameplanIndexes = builder.execute()
            saveJSON.put("battleorder", gameplanIndexes)
        }
    }

    fun LeagueCreator.DraftOrder(builder: DraftOrderCreator.() -> Unit) {
        DraftOrder(fromLeagueCreatorDraft(builder))
    }

    inner class DraftOrder(private val builder: DraftOrderCreator) : Addon {
        init {
            addonList += this
        }

        override suspend fun LeagueCreator.execute(b: RequestBuilder) {
            builder.requestBuilder = b
            saveJSON.put("originalorder", builder.execute())
        }
    }

    inner class MatchUps(builder: MatchUps.() -> Unit) : Addon {

        init {
            builder()
            addonList += this
        }

        lateinit var mainPlayerNameCoord: (Int) -> Coord
        lateinit var mainPlayerMonCoord: (Int) -> Coord
        lateinit var opponentNameCoord: (Int, Int) -> Coord
        lateinit var opponentMonCoord: (Int, Int) -> Coord

        override suspend fun LeagueCreator.execute(b: RequestBuilder) {
            repeat(playerCount) { player ->
                val mainPlayer = playerNameIndexer(player).formula
                val mainMonCoord = teamsiteMonCoord!!(player)
                b.addSingle(mainPlayerNameCoord(player).toString(), mainPlayer)
                b.addSingle(
                    mainPlayerMonCoord(player).toString(),
                    "=SORT($mainMonCoord:${(mainMonCoord + (1 to (monCount - 1))).withoutSheet};2;0)"
                )
                for (gdi in 0 until gamedays) {
                    val gameday = gdi + 1
                    val lists = gameplanIndexes!![gameday] ?: continue
                    val opponent = lists.first { player in it }.first { it != player }
                    val opponentPlayer = playerNameIndexer(opponent).formula
                    b.addSingle(opponentNameCoord(player, gdi).toString(), opponentPlayer)
                    b.addSingle(opponentMonCoord(player, gdi).toString(),
                        teamsiteMonCoord!!(opponent).let { "=SORT($it:${(it + (1 to (monCount - 1))).withoutSheet};2;0)" })
                }
            }
        }

    }

    inner class KDOnTeamSite(builder: KDOnTeamSite.() -> Unit) : Addon {


        var headercols: List<Pair<HeaderCol, String>>? = null
        fun defaultHeaderCols() {
            headercols = defaultHeaderCols
        }

        val defaultHeaderCols =
            mutableMapOf(HeaderCol.GAMEDAY to "%s. Spieltag", HeaderCol.OPPONENT to "vs. %s").toList()
        lateinit var datacols: List<Cols>
        var gapBetweenGamedays = 0
        var iferror = ""
        lateinit var beginHeader: (Int) -> Coord
        var beginData: ((Int) -> Coord)? = null

        init {
            builder()
            addonList += this
        }

        override suspend fun LeagueCreator.execute(b: RequestBuilder) {
            val killStart = 3
            val deathStart = gamedays + 5
            repeat(playerCount) { i ->
                val monCoord = teamsiteMonCoord!!(i)
                println("DATACOLS SIZE $datacols")

                (gameplanIndexes?.keys?.sorted()?.map { it - 1 } ?: (0 until gamedays)).forEach { gdi ->
                    if (headercols != null) {
                        println("HEADERCOLS NOT NULL")
                        b.addRow(beginHeader(i).run { "$sheet!${gdi.x(datacols.size + gapBetweenGamedays, x)}$y" },
                            headercols!!.map { (key, value) ->
                                when (key) {
                                    HeaderCol.GAMEDAY -> value.format(gdi + 1)
                                    HeaderCol.OPPONENT -> {
                                        if (gameplanIndexes?.get(gdi + 1) == null) return@map ""
                                        "=\"" + value.format(gameplanIndexes?.let { map ->
                                            "\"&" + playerNameIndexer(map[gdi + 1]!!.first { it.contains(i) }
                                                .let { l -> l.first { it != i } })
                                        } ?: "")
                                    }

                                    HeaderCol.OTHER -> value
                                }
                            })
                    }
                    beginData?.let { f ->
                        var currentDeathCol: String
                        var currentYValues: Pair<Int, Int>
                        b.addRow(f(i).also {
                            currentDeathCol = gdi.x(datacols.size + gapBetweenGamedays, it.x + datacols.indexOf(DEATHS))
                            currentYValues = it.y to (it.y + monCount + slotsOnTeamsite - 1)
                        }.run { "$sheet!${gdi.x(datacols.size + gapBetweenGamedays, x)}$y" }, datacols.map {
                            when (it) {
                                //KILLS -> "=$dataSheet!${(gamedays + 2).xc()}${i.y(gap, )}"
                                KILLS -> "=ARRAYFORMULA(WENNFEHLER(SVERWEIS(${monCoord.withoutSheet}:${
                                    monCoord.plusY(
                                        monCount - 1 + slotsOnTeamsite
                                    ).withoutSheet
                                };$dataSheet!\$B$${i.y(gap, 3)}:\$${gdi.x(1, killStart)}\$${
                                    i.y(
                                        gap, dataMonCount + 1
                                    )
                                };${gdi + killStart - 1};0); \"$iferror\"))"

                                DEATHS -> "=ARRAYFORMULA(WENNFEHLER(SVERWEIS(${monCoord.withoutSheet}:${
                                    monCoord.plusY(
                                        monCount - 1 + slotsOnTeamsite
                                    ).withoutSheet
                                };$dataSheet!\$B\$${i.y(gap, 3)}:\$${gdi.x(1, deathStart)}\$${
                                    i.y(
                                        gap, dataMonCount + 2
                                    )
                                };${gdi + deathStart - 1};0); \"$iferror\"))"

                                USES -> "=ARRAYFORMULA(WENN(${currentDeathCol}${currentYValues.first}:${currentDeathCol}${currentYValues.second} = \"$iferror\"; \"$iferror\";1))"

                                else -> error("Invalid column at KDOnTeamSite $it")
                            }
                        })
                    }
                }
            }
        }
    }

    inner class ShowdownScript(val format: ScriptFormat = ScriptFormat.GEN9NATDEXAG) : Addon {
        init {
            addonList += this
        }

        override suspend fun LeagueCreator.execute(b: RequestBuilder) {
            Templater.ShowdownScriptTemplate(this).build {
                name = this@execute.name + "Tierlist"
                format = this@ShowdownScript.format
                tierset = Json.encodeToString(
                    tierlist.groupBy { it.tier.substringBefore("#") }.toList()
                        .sortedBy { Tierlist[guild]!!.order.indexOf(it.first) }.flatMap { (tier, list) ->
                            listOf(listOf("header", "$tier-Tier"), *list.map {
                                listOf(
                                    "pokemon", NameConventionsDB.getDiscordTranslation(
                                        it.name, guild, english = true
                                    )!!.official.toSDName()
                                )
                            }.sortedBy { it[1] }.toTypedArray<List<String>>()
                            )
                        })
            }
        }
    }

    enum class HeaderCol {
        GAMEDAY, OPPONENT, OTHER
    }

    sealed class DocUsedCols {
        abstract fun PokemonData.getFormula(area: String): String
    }

    sealed class CalcedCol : DocUsedCols() {
        class Points(private val tierToPointArea: String) : CalcedCol() {
            override fun PokemonData.getFormula(area: String): String {
                return "=ARRAYFORMULA(WENNFEHLER(SVERWEIS(${
                    getDefaultVLookup(
                        area, DataCol.TIER
                    )
                };$tierToPointArea;2;0)))"
            }
        }
    }

    abstract class DataCol : DocUsedCols() {

        override fun PokemonData.getFormula(area: String): String {
            //return "=ARRAYFORMULA(WENNFEHLER())"
            return "=ARRAYFORMULA(WENNFEHLER(${getDefaultVLookup(area, this@DataCol)}))"
        }

        data object SPEED : DataCol() {
            override fun getData(o: Pokemon) = o.speed.toString()
        }

        data object TYPE1 : DataCol() {
            override fun getData(o: Pokemon): String =
                typeicons.optString(o.types.getOrNull(0).orEmpty()).ifEmpty { "/" }
        }

        data object TYPE2 : DataCol() {
            override fun getData(o: Pokemon): String =
                typeicons.optString(o.types.getOrNull(1).orEmpty()).ifEmpty { "/" }
        }

        data object TIER : DataCol() {
            override fun getData(o: Pokemon): String {
                error("Not implemented because its implemented above")
            }
        }

        data object ENGLISHTLNAME : DataCol() {
            override fun getData(o: Pokemon) = error("Not implemented because its implemented above")
        }

        data object GEN5SPRITE : DataCol() {
            override fun getData(o: Pokemon) = Command.getGen5Sprite(o)
        }

        data object ICON : DataCol() {
            override fun getData(o: Pokemon): String {


                //println(it)
                //println(NameConventionsDB.getDiscordTranslation(it, 1234567)!!.official.toSDName())
                val str = specialCases[o.name] ?: ("${o.num}".padStart(3, '0') + (o.forme?.split("-")?.let { arr ->
                    if (arr[0] in specialForms) arr.last() else arr.first()
                }?.substring(0, 1)?.lowercase()?.let { x -> "-$x" } ?: ""))
                return "=IMAGE(\"https://www.serebii.net/pokedex-${/*if (str.split("-")[0].toInt() > 905 || o.name in gen9Names) */"sv" /*else "swsh"*/}/icon/$str.png\";1)"
            }

        }

        abstract fun getData(o: Pokemon): String

        companion object {
            private val specialForms = listOf("Alola", "Galar", "Mega", "Paldea")
            private val specialCases = mapOf("Rotom-Fan" to "479-s", "Tauros-Paldea-Combat" to "128-p")
            private val gen9Names =
                listOf("Tauros-Paldea-Combat", "Tauros-Paldea-Blaze", "Tauros-Paldea-Aqua", "Wooper-Paldea")

            val typeicons by lazy { Utils.load("typeicons.json") }
            /*fun TYPES(wrappedAsImage: Boolean, placeholder: String = "/"): Array<DataCol> = List(2) {
                object : DataCol() {
                    override fun getData(o: Pokemon): String {
                        val type = o.types.getOrNull(it) ?: return placeholder
                        return if (wrappedAsImage) {
                            typeicons.optString(type) ?: error("No typeicon for $type")
                        } else {
                            type
                        }
                    }
                }
            }.toTypedArray()*/

        }
    }

}

private fun fill(x: Int, y: Int, filler: String) = List(y) { List(x) { filler } }
fun List<Cols>.columnFrom(col: Cols) = getAsXCoord(indexOf(col) + 2)
fun ((Int) -> Coord).asStringIndexer(): (Int) -> String = { "=" + this(it).toString().replace("=", "") }

fun cellFormat(strikethrough: Boolean? = null, fgColor: Color? = null, bgColor: Color? = null) = CellFormat().apply {
    if (strikethrough != null) textFormat = TextFormat().apply { this.strikethrough = strikethrough }
    if (fgColor != null) textFormat = (textFormat ?: TextFormat()).apply { foregroundColor = fgColor }
    if (bgColor != null) backgroundColor = bgColor

}

@Suppress("unused")
object ConditionalFormats {
    val strikethrough = cellFormat(strikethrough = true)
    val redAndStrikethrough = cellFormat(strikethrough = true, fgColor = convertColor(0xFF0000))
    val red = cellFormat(fgColor = convertColor(0xFF0000))
}
