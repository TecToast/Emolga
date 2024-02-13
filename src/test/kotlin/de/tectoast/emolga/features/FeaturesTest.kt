package de.tectoast.emolga.features

import de.tectoast.emolga.bot.EmolgaMain
import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.keepAlive
import de.tectoast.emolga.utils.Constants
import dev.minn.jda.ktx.events.listener
import io.kotest.core.spec.style.FunSpec
import mu.KotlinLogging
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.interactions.commands.Command
import kotlin.time.TimeSource

var timer: TimeSource.Monotonic.ValueTimeMark? = null
private val logger = KotlinLogging.logger {}

class FeaturesTest : FunSpec({
    test("test") {
        println("TEST")
//        val manager = FeatureManager(setOf(TestFeature, TestButton, TestModal, TestMenu, NestedCommand))
        val manager = EmolgaMain.featureManager
        jda.listener<GenericEvent> {
            timer = TimeSource.Monotonic.markNow()
            manager.handleEvent(it)
            logger.info("Handled ${it::class.simpleName} in ${timer!!.elapsedNow()}")

        }
        manager.updateFeatures(jda, updateGuilds = listOf(Constants.G.MY))

//        defaultChannel.send("test", components = TestMenu(argsBuilder = {
//            test1 = "lololo"
//            test2 = "lalala"
//        }) {
//            option("test10", "test10")
//            option("test20", "test20")
//        }.into()).queue()
//        jda.getGuildById(Constants.G.MY)!!.upsertCommand("test", "test").addOptions(
//            OptionData(OptionType.STRING, "test1", "test1"), OptionData(OptionType.STRING, "test2", "test2")
//        ).queue()
//        defaultChannel.send("hehehehehe", components = primary(TestButton.createComponentId {
//            test1 = "testlooooool"
//        }, ":)").into()).queue()

        keepAlive()
    }
})

object TestFeature : CommandFeature<TestFeature.Args>(::Args, CommandSpec("testabc", "test")) {
    class Args : Arguments() {
        var test3 by boolean("test3", "test3") {
            validate {
                if (!it) throw IllegalArgumentException("test3 muss true sein!")
                true
            }
        }
        var test4 by string("test4", "trest4") {
            slashCommand(listOf(Command.Choice("YeeHaa", "lol1"), Command.Choice("YeeHaa2", "lol2")))
        }
        var test1 by string("test1", "test1").nullable()
        var test2 by string("test2", "test2") {
            default { "$test1!!!$test4" }
            slashCommand { s, _ ->
                listOf("test1", "test2", "test3").filter { it.startsWith(s) }
            }
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Args) {
        reply("test1: ${e.test1}, test2: ${e.test2}")
        if (e.test3) println(":DDDDDDDDDDDDDDDDD")
        println("GuMo ${timer!!.elapsedNow()}")
    }
}

object NestedCommand : CommandFeature<Arguments>(::Arguments, CommandSpec("nested", "nested")) {
    object First : CommandFeature<First.Args>(::Args, CommandSpec("first", "first :3")) {
        class Args : Arguments() {
            var test1 by string("test1", "test1") {
                validate {
                    if (it == "test") throw InvalidArgumentException("test1 darf nicht test sein!")
                    it
                }
            }.nullable()
            var test2 by string("test2", "test2") {
                default { "$test1!!!" }
                validate { null }
                customErrorMessage = "I like trains :)"
            }
        }

        context(InteractionData) override suspend fun exec(e: Args) {
            reply("FIRST test1: ${e.test1}, test2: ${e.test2}")
            println("GuMo ${timer!!.elapsedNow()}")
        }
    }

    object Second : CommandFeature<Second.Args>(::Args, CommandSpec("second", "second :3")) {
        class Args : Arguments() {
            var test1 by string("test3", "test6").nullable()
            var test2 by string("test4", "test5") {
                default { "$test1!!!" }
            }
        }

        context(InteractionData) override suspend fun exec(e: Args) {
            reply("SECOND test1: ${e.test1}, test2: ${e.test2}")
            println("GuMo ${timer!!.elapsedNow()}")
        }
    }

    context(InteractionData)
    override suspend fun exec(e: Arguments) {

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
