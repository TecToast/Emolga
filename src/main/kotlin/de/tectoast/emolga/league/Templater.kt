package de.tectoast.emolga.league

import de.tectoast.emolga.commands.file


sealed class Templater(private val leagueCreator: LeagueCreator, val templatePath: String) {
    val templateValues = mutableMapOf<String, String>()
    val filename by lazy {
        changeFileName(
            "${leagueCreator.filename.substringBefore(".")}.${templatePath.substringAfterLast(".")}".lowercase()
        )
    }

    fun save() {
        // Creating template
        var template = "templates/$templatePath".file().readText()
        template = beforeReplace(template)
        templateValues.forEach { (key, value) -> template = template.replace("###$key###", value) }
        template = afterReplace(template)
        "templatesfinished/$filename".file().writeText(template)
    }

    class ShowdownScriptTemplate(leagueCreator: LeagueCreator) :
        Templater(leagueCreator, "showdownscript.js") {
        var name: String by templateValues
        var tierset: String by templateValues
        lateinit var format: Format


        inline fun build(builder: ShowdownScriptTemplate.() -> Unit) {
            apply(builder)
            templateValues["namespace"] = name.replace(" ", "").lowercase()
            templateValues["url"] =
                "https://emolga.tectoast.de/showdownscripts/$filename"
            templateValues["format"] = format.formatName
            templateValues["formatSlices"] = format.data.replace
            save()
        }

        override fun beforeReplace(template: String): String {
            var t = template
            if (format.data.noFormat) {
                t = t.replace("[format]", "")
            }
            return t
        }

        enum class Format(val formatName: String, val data: FormatData) {
            GEN9NATDEXAG("gen9natdex", FormatData.LADDER), GEN7("gen7", FormatData.LADDER), GEN9NATDEXDRAFT(
                "NULL",
                FormatData.DRAFT
            )

        }

        enum class FormatData(val replace: String, val noFormat: Boolean) {
            LADDER(normalLadderWorks, false), DRAFT(natdexDraft, true)
        }

        companion object {
            private val normalLadderWorks = """
                    BattleTeambuilderTable[format].formatSlices["CAP LC"] = 0;
                    BattleTeambuilderTable[format].formatSlices["AG"] = 0;
                    BattleTeambuilderTable[format].formatSlices["Uber"] = 0;""".trimIndent()
            private val natdexDraft = """
                for(let x of Object.keys(BattleTeambuilderTable.formatSlices)) BattleTeambuilderTable.formatSlices[x] = 0;
            """.trimIndent()
        }

        override fun changeFileName(original: String) = "${original.substringBeforeLast(".")}.user.js"
    }

    open fun changeFileName(original: String) = original
    open fun beforeReplace(template: String) = template
    open fun afterReplace(template: String) = template


}
