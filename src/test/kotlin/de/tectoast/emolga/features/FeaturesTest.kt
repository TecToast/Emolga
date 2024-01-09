package de.tectoast.emolga.features

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.commands.InteractionData
import de.tectoast.emolga.defaultChannel
import de.tectoast.emolga.keepAlive
import dev.minn.jda.ktx.events.listener
import dev.minn.jda.ktx.interactions.components.option
import dev.minn.jda.ktx.messages.into
import dev.minn.jda.ktx.messages.send
import io.kotest.core.spec.style.FunSpec
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import kotlin.time.TimeSource

var timer: TimeSource.Monotonic.ValueTimeMark? = null

class FeaturesTest : FunSpec({
    test("test") {
        println("TEST")
        val manager = FeatureManager(setOf(TestFeature, TestButton, TestModal, TestMenu))
        jda.listener<GenericInteractionCreateEvent> {
            timer = TimeSource.Monotonic.markNow()
            manager.handleEvent(it)
        }
        defaultChannel.send("test", components = TestMenu(argsBuilder = {
            test1 = "lololo"
            test2 = "lalala"
        }) {
            option("test10", "test10")
            option("test20", "test20")
        }.into()).queue()
//        jda.getGuildById(Constants.G.MY)!!.upsertCommand("test", "test").addOptions(
//            OptionData(OptionType.STRING, "test1", "test1"), OptionData(OptionType.STRING, "test2", "test2")
//        ).queue()
//        defaultChannel.send("hehehehehe", components = primary(TestButton.createComponentId {
//            test1 = "testlooooool"
//        }, ":)").into()).queue()

        keepAlive()
    }
})

object TestFeature : CommandFeature<TestFeature.Args>(::Args, CommandSpec("test", "test")) {
    class Args : Arguments() {
        var test1 by string("test1", "test1").nullable()
        var test2 by string("test2", "test2") {
            default { "$test1!!!" }
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        reply("test1: ${e.test1}, test2: ${e.test2}")
        println("GuMo ${timer!!.elapsedNow()}")
    }
}

object TestButton : ButtonFeature<TestButton.Args>(::Args, ButtonSpec("test")) {
    class Args : Arguments() {
        var test1 by string("test1", "test1").nullable()
        var test2 by string("test2", "test2") {
            default { "$test1!!!" }
        }
    }

    context(InteractionData) override suspend fun exec(e: Args) {
        replyModal(TestModal())
        println("GuMo ${timer!!.elapsedNow()}")
    }
}

object TestModal : ModalFeature<TestModal.Args>(::Args, ModalSpec("test")) {
    override val title = "Test :)"

    class Args : Arguments() {
        var test1 by string("test1", "test1") {
            modal {
                placeholder = ":)"
            }
        }.nullable()
        var test2 by string("test2", "test2") {
            default { "$test1!!!" }
        }
    }

    context(InteractionData) override suspend fun exec(e: Args) {
        reply("test1: ${e.test1}, test2: ${e.test2}")

        println("GuMo ${timer!!.elapsedNow()}")
    }

}

object TestMenu : SelectMenuFeature<TestMenu.Args>(::Args, SelectMenuSpec("test")) {
    class Args : Arguments() {
        var test1 by string("test1", "test1").compIdOnly().nullable()
        var test2 by string("test2", "test2") {
            default { "$test1!!!" }
        }.compIdOnly()
        var option by singleOption()
    }

    context(InteractionData) override suspend fun exec(e: Args) {
        reply("test1: ${e.test1}, test2: ${e.test2}, option: ${e.option}")
        println("GuMo ${timer!!.elapsedNow()}")
    }

}
