package de.tectoast.emolga.utils

import com.google.api.services.sheets.v4.model.Color
import de.tectoast.emolga.database.exposed.TranslationsDB
import de.tectoast.emolga.features.flo.SendFeatures.sendToMe
import de.tectoast.emolga.utils.Constants.FLOID
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.Column
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.io.File
import java.text.SimpleDateFormat
import kotlin.collections.set
import kotlin.math.pow

fun <T> T.indexedBy(list: List<T>) = list.indexOf(this)
val embedColor = java.awt.Color.CYAN.rgb

/**
 * Converts an integer to a column name in Google Sheets.
 *
 * Examples: `1 -> A, 3 -> C, 27 -> AA, 28 -> AB, 53 -> BA`
 * @param xc the column number
 * @return the column name
 */
fun getAsXCoord(xc: Int): String {
    var x = 0
    var toResolve = xc
    while (true) {
        val powed = 26.0.pow(x).toInt()
        if (toResolve < powed) break
        toResolve -= powed
        x++
    }
    return buildString {
        for (i in 0 until x) {
            append(((toResolve % 26) + 65).toChar())
            toResolve /= 26
        }
        reverse()
    }
}

fun Int.x(factor: Int, summand: Int) = getAsXCoord(y(factor, summand))
fun Int.xdiv(divident: Int, factor: Int, summand: Int) = getAsXCoord(ydiv(divident, factor, summand))
fun Int.xmod(mod: Int, factor: Int, summand: Int) = getAsXCoord(ymod(mod, factor, summand))

fun Int.xc() = getAsXCoord(this)

fun Int.y(factor: Int, summand: Int) = this * factor + summand
fun Int.ydiv(divident: Int, factor: Int, summand: Int) = (this / divident) * factor + summand
fun Int.ymod(mod: Int, factor: Int, summand: Int) = (this % mod) * factor + summand
fun coord(sheet: String, x: String, y: Int) = "$sheet!$x$y"
fun Int.coordXMod(sheet: String, num: Int, xFactor: Int, xSummand: Int, yFactor: Int, ySummand: Int) =
    coord(sheet, this.xmod(num, xFactor, xSummand), this.ydiv(num, yFactor, ySummand))

fun Int.coordYMod(sheet: String, num: Int, xFactor: Int, xSummand: Int, yFactor: Int, ySummand: Int) =
    coord(sheet, this.xdiv(num, xFactor, xSummand), this.ymod(num, yFactor, ySummand))


private val SD_NAME_PATTERN = Regex("[^a-zA-Z\\däöüÄÖÜß♂♀é+]+")
private val USERNAME_PATTERN = Regex("[^a-zA-Z\\d]+")
fun String.toSDName() = SD_NAME_PATTERN.replace(this.lowercase().replace('é', 'e'), "")
fun String.toUsername() = USERNAME_PATTERN.replace(
    lowercase().trim().replace("ä", "a").replace("ö", "o").replace("ü", "u").replace("ß", "ss"), ""
)

fun Int.convertColor(): Color {
    val c = java.awt.Color(this)
    return Color().setRed(c.red.toFloat() / 255f).setGreen(c.green.toFloat() / 255f).setBlue(c.blue.toFloat() / 255f)
}

val webJSON = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = false
}

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(webJSON)
    }
}

val defaultScope = CoroutineScope(Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, t ->
    t.printStackTrace()
    sendToMe("Error in default scope, look in console")
})
val myJSON = Json {
    prettyPrint = true
}

val otherJSON = Json {
    isLenient = true
    ignoreUnknownKeys = true
}


val defaultTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm")
fun String.file() = File(this)

val String.marker: Marker get() = MarkerFactory.getMarker(this)

fun String.condAppend(check: Boolean, value: String) = if (check) this + value else this
inline fun String.condAppend(check: Boolean, value: () -> String) = if (check) this + value() else this

fun String.notNullAppend(value: String?) = if (value != null) this + value else this

val <T> T.l get() = listOf(this)

inline val User.isFlo: Boolean get() = this.idLong == FLOID
inline val User.isNotFlo: Boolean get() = this.idLong != FLOID
inline fun String.ifNotEmpty(block: (String) -> String): String {
    return if (this.isNotEmpty()) block(this) else this
}

fun String.surroundWith(surround: String) = surround + this + surround

inline fun <T> Collection<T>.randomWithCondition(condition: (T) -> Boolean) = this.filter(condition).randomOrNull()

fun <K> MutableMap<K, Int>.add(key: K, value: Int) = compute(key) { _, v ->
    v?.plus(value) ?: value
}

operator fun <K, V> Map<K, V>.invoke(key: K) = getValue(key)
fun <T> Collection<T>.filterStartsWithIgnoreCase(other: String, tostring: (T) -> String = { it.toString() }) =
    mapNotNull {
        val str = tostring(it)
        if (str.startsWith(other, ignoreCase = true)) str else null
    }

fun Double.roundToDigits(digits: Int) = "%.${digits}f".format(this)

enum class Language(val translationCol: Column<String>, val otherCol: Column<String>) {
    GERMAN(TranslationsDB.GERMANNAME, TranslationsDB.ENGLISHNAME), ENGLISH(
        TranslationsDB.ENGLISHNAME, TranslationsDB.GERMANNAME
    )
}

data class Translation(
    val translation: String,
    val type: Type,
    val language: Language,
    val otherLang: String = "",
    val forme: String? = null
) {
    val isEmpty: Boolean

    companion object {
        private val emptyTranslation: Translation = Translation("", Type.UNKNOWN, Language.GERMAN)

        /**
         * Cache for german translations
         */
        val translationsCacheGerman = SizeLimitedMap<String, Translation>(200)

        /**
         * Cache for english translations
         */
        val translationsCacheEnglish = SizeLimitedMap<String, Translation>(200)

        private fun empty(): Translation {
            return emptyTranslation
        }

        fun getGerName(s: String): Translation {
            val id = s.toSDName()
            if (translationsCacheGerman.containsKey(id)) return translationsCacheGerman.getValue(id)
            return TranslationsDB.getTranslation(id, false, Language.GERMAN)?.also {
                addToCache(true, id, it)
            } ?: empty()
        }

        fun getEnglNameWithType(s: String): Translation {
            val id = s.toSDName()
            if (translationsCacheEnglish.containsKey(id)) return translationsCacheEnglish.getValue(id)
            return TranslationsDB.getTranslation(id, false, Language.ENGLISH)?.also {
                addToCache(false, id, it)
            } ?: empty()
        }

        private fun addToCache(german: Boolean, sd: String, t: Translation) {
            if (german) {
                translationsCacheGerman[sd] = t
            } else {
                translationsCacheEnglish[sd] = t
            }
        }
    }

    enum class Type(val id: String, private val typeName: String, private val female: Boolean) {
        ABILITY("abi", "Fähigkeit", true), EGGGROUP("egg", "Eigruppe", true), ITEM(
            "item", "Item", false
        ),
        MOVE("atk", "Attacke", true), NATURE("nat", "Wesen", false), POKEMON("pkmn", "Pokémon", false), TYPE(
            "type", "Typ", false
        ),
        TRAINER("trainer", "Trainer", false), UNKNOWN("unknown", "Undefiniert", false);

        companion object {
            fun fromId(id: String): Type {
                return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: UNKNOWN
            }
        }
    }

    init {
        this.isEmpty = type == Type.UNKNOWN
    }
}
