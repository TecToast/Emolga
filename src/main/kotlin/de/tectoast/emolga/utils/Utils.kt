package de.tectoast.emolga.utils

import com.google.api.services.sheets.v4.model.Color
import com.mongodb.MongoWriteException
import de.tectoast.emolga.database.exposed.NameConventionsDB
import de.tectoast.emolga.database.exposed.TranslationsDB
import de.tectoast.emolga.utils.Constants.FLOID
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.Column
import org.slf4j.Marker
import org.slf4j.MarkerFactory
import java.io.File
import java.text.SimpleDateFormat
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

val universalLogger = KotlinLogging.logger("Universal")
private val basicCoroutineContext = SupervisorJob() + CoroutineExceptionHandler { ctx, t ->
    val name = ctx[CoroutineName]?.name ?: "Unknown"
    universalLogger.error(t) { "Error in $name" }
}

fun createCoroutineScope(name: String, dispatcher: CoroutineDispatcher = Dispatchers.Default) =
    CoroutineScope(createCoroutineContext(name, dispatcher))

fun createCoroutineContext(name: String, dispatcher: CoroutineDispatcher = Dispatchers.Default) =
    basicCoroutineContext + dispatcher + CoroutineName(name)

val webJSON = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    serializersModule = SerializersModule {
        contextual(Long::class, LongAsStringSerializer)
    }
}

val httpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(webJSON)
    }
}

val defaultScope = createCoroutineScope("Default")
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

fun String.notNullAppend(value: String?) = notNullAppend(value) { it }
inline fun <T> String.notNullAppend(value: T?, mapper: (T) -> String) =
    if (value != null) this + mapper(value) else this

inline fun <T> String.notNullPrepend(value: T?, mapper: (T) -> String) =
    if (value != null) mapper(value) + this else this

val <T> T.l get() = listOf(this)

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
inline fun <T> Collection<T>.filterStartsWithIgnoreCase(other: String, tostring: (T) -> String = { it.toString() }) =
    mapNotNull {
        val str = tostring(it)
        if (str.startsWith(other, ignoreCase = true)) str else null
    }

inline fun <T> Collection<T>.filterContainsIgnoreCase(other: String, tostring: (T) -> String = { it.toString() }) =
    mapNotNull {
        val str = tostring(it)
        if (str.contains(other, ignoreCase = true)) str else null
    }


fun Double.roundToDigits(digits: Int) = "%.${digits}f".format(this)

inline fun Boolean.ifTrue(block: () -> Unit): Boolean {
    if (this) block()
    return this
}

/**
 * Ignores an exception if the predicate returns true
 * @return true if the operation succeeded normally, false if the exception was ignored
 * @throws Throwable the exception if the predicate returns false
 */
inline fun ignoreException(predicate: (Throwable) -> Boolean = { true }, block: () -> Unit): Boolean {
    return try {
        block()
        true
    } catch (e: Throwable) {
        if (predicate(e)) false
        else throw e
    }
}

/**
 * Ignores an duplicate entry exception
 * @return true if the operation succeeded normally, false if the element was there
 * @throws Throwable the exception if the predicate returns false
 */
inline fun ignoreDuplicatesMongo(block: () -> Unit) =
    ignoreException({ it is MongoWriteException && it.code == 11000 }, block)

fun <T> Iterable<T>.reversedIf(condition: Boolean) = if (condition) reversed() else this.toList()

enum class Language(
    val translationCol: Column<String>,
    val otherCol: Column<String>,
    val ncSpecifiedCol: Column<String>
) {
    GERMAN(TranslationsDB.GERMANNAME, TranslationsDB.ENGLISHNAME, NameConventionsDB.SPECIFIED),
    ENGLISH(TranslationsDB.ENGLISHNAME, TranslationsDB.GERMANNAME, NameConventionsDB.SPECIFIEDENGLISH)
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

        suspend fun getGerName(s: String): Translation {
            val id = s.toSDName()
            if (translationsCacheGerman.containsKey(id)) return translationsCacheGerman.getValue(id)
            return TranslationsDB.getTranslation(id, false, Language.GERMAN)?.also {
                addToCache(true, id, it)
            } ?: empty()
        }

        suspend fun getEnglNameWithType(s: String): Translation {
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

val String.isMega get() = "-Mega" in this
