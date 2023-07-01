package de.tectoast.emolga.utils.automation.structure

import de.tectoast.emolga.commands.Command
import de.tectoast.emolga.utils.json.Configuration
import de.tectoast.emolga.utils.json.db
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.modals.Modal
import org.litote.kmongo.coroutine.updateOne
import org.litote.kmongo.eq
import java.awt.Color
import java.util.function.Function

class ModalConfigurator private constructor() {
    private val actionRows: MutableList<TextInput> = mutableListOf()
    private val mapper: MutableMap<String, Function<String, Any?>> = mutableMapOf()
    private var id: String? = null
    private var title: String? = null


    fun buildModal(page: Int = 0): Modal {
        return Modal.create("modalconfigurator;$id", title!!)
            .addComponents(
                actionRows.drop(page * 5).take(5).map { ActionRow.of(it) })
            .build()
    }

    suspend fun handle(e: ModalInteractionEvent) {
        val values = e.values
        val gid = e.guild!!.idLong
        val configuration = db.configuration.findOne(Configuration::guild eq gid) ?: Configuration(gid)
        val o = configuration.data.getOrPut(id!!) { mutableMapOf() }
        val member = e.member!!
        for (mm in values) {
            val id = mm.id
            val value = mm.asString
            if (value.isBlank()) continue
            val mappedValue = if (mapper.containsKey(id)) mapper[id]!!.apply(value) else value
            if (mappedValue == null) {
                e.replyEmbeds(
                    EmbedBuilder()
                        .setTitle(
                            "Das Argument $value ist ungültig für \"${actionRows.first { it.id == id }.label}\"!"
                        )
                        .setColor(0xFF0000)
                        .setFooter("Aufgerufen von ${member.effectiveName} (${member.user.effectiveName})")
                        .build()
                ).queue()
                return
            }
            o[id] = mappedValue.toString().toIntOrNull() ?: run {
                Command.sendToMe("$mappedValue in $id is not a long")
                return
            }
        }
        e.replyEmbeds(
            EmbedBuilder()
                .setTitle("Deine Konfiguration wurde erfolgreich gespeichert!")
                .setColor(0x00FF00)
                .setFooter("Aufgerufen von ${member.effectiveName} (${member.user.effectiveName}")
                .build()
        ).queue()
        db.configuration.updateOne(configuration)
    }

    fun initialize(e: SlashCommandInteractionEvent) {
        if (actionRows.size <= 5) {
            e.replyModal(buildModal()).queue()
            return
        }
        val embed = EmbedBuilder()
            .setTitle("Welche Seite möchtest du sehen?")
            .setColor(Color.CYAN)
        val sm = StringSelectMenu.create("modalconfigurator;$id")
        for (i in 0 until actionRows.size / 5 + 1) {
            val realSite = i + 1
            embed.addField(
                "Seite $realSite",
                actionRows.drop(i * 5).take(5)
                    .joinToString("\n") { obj: TextInput -> obj.label }, false
            )
            sm.addOption("Seite $realSite", i.toString())
        }
        e.replyEmbeds(embed.build()).addActionRow(sm.build()).queue()
    }

    fun id(id: String?): ModalConfigurator {
        this.id = id
        return this
    }

    fun title(title: String?): ModalConfigurator {
        this.title = title
        return this
    }

    fun actionRows(vararg actionRows: TextInput): ModalConfigurator {
        this.actionRows.addAll(listOf(*actionRows))
        return this
    }

    fun actionRows(actionRows: List<TextInput>): ModalConfigurator {
        this.actionRows.addAll(actionRows)
        return this
    }

    fun mapper(mapper: Function<String, Any?>, vararg ids: String): ModalConfigurator {
        for (s in ids) {
            this.mapper[s] = mapper
        }
        return this
    }

    override fun toString(): String {
        return "ModalConfigurator{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}'
    }

    companion object {
        fun create(): ModalConfigurator {
            return ModalConfigurator()
        }
    }
}
