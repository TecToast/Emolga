package de.tectoast.emolga.features.flo

import de.tectoast.emolga.features.*
import de.tectoast.emolga.utils.file
import java.text.SimpleDateFormat

object ESSNews {
    object Command : CommandFeature<NoArgs>(NoArgs(), CommandSpec("essnews", "ESS News", 1030585128696680538)) {
        init {
            restrict(roles(1030585814800932984))
        }

        context(InteractionData)
        override suspend fun exec(e: NoArgs) {
            replyModal(Modal())
        }
    }

    object Modal : ModalFeature<Modal.Args>(::Args, ModalSpec("essnews")) {
        override val title = "ESS News"

        class Args : Arguments() {
            var title by string("Titel") {
                modal {
                    setRequiredRange(1, 50)
                }
            }
            var text by string("Text") {
                modal(short = false) {
                    setRequiredRange(1, 300)
                }
            }
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val title = e.title
            val text = e.text
            val line =
                """								<div class="newsentry"><h4>$title</h4><p>$text</p><p>&mdash;<strong>ESS Team</strong> <small class="date">am $currentDate</small></p></div>"""
            paths.forEach {
                it.writeText(it.readLines().toMutableList().apply {
                    set(38, line)
                }.joinToString("\n"))
            }
            reply("ESS News wurden geupdated!")
        }
    }

    val paths = listOf(
        "/var/www/essclient/index.html".file(),
        "/var/www/essclient/index.template.html".file()
    )
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy")
    private val currentDate: String
        get() = dateFormat.format(System.currentTimeMillis())
}
