package de.tectoast.emolga.utils

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.commands.dexquiz.DexQuizTip
import de.tectoast.emolga.utils.config.ConfigSource
import java.util.*
import java.util.function.Function

enum class ConfigManager(private val defaultValue: Function<String, Any>) {
    DEXQUIZ(Function<String, Any> { s: String ->
        if (s == "totalbudget") return@Function Command.DEXQUIZ_BUDGET
        DexQuizTip.valueOf(s).defaultPrice
    });

    private val configSource: ConfigSource = ConfigSource.JSON

    fun getValue(gid: Long, key: String): Any {
        return configSource[gid, name.lowercase(Locale.getDefault()), key, { defaultValue.apply(key) }]
    }
}