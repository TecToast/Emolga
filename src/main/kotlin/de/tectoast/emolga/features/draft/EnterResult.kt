package de.tectoast.emolga.features.draft

import de.tectoast.emolga.bot.jda
import de.tectoast.emolga.database.exposed.ResultCodesDB
import de.tectoast.emolga.features.*
import de.tectoast.emolga.league.League
import de.tectoast.emolga.utils.Constants
import de.tectoast.emolga.utils.ResultEntry
import de.tectoast.emolga.utils.StateStore
import de.tectoast.emolga.utils.draft.DraftPlayer
import de.tectoast.emolga.utils.json.db
import de.tectoast.emolga.utils.json.emolga.reverseGet
import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission

object EnterResult {

    object ResultCommand : CommandFeature<ResultCommand.Args>(
        ::Args, CommandSpec(
            "result", "Startet die interaktive Ergebniseingabe", Constants.G.VIP, Constants.G.COMMUNITY,
            Constants.G.EPP, Constants.G.LOEWE, Constants.G.ADK, Constants.G.NPL, Constants.G.GPC
        )
    ) {
        private val nameCache = mutableMapOf<String, Map<Long, String>>() // guild -> uid -> name
        private val leagueCache = mutableMapOf<Long, String>()

        class Args : Arguments() {
            var opponent by fromList("Gegner", "Dein Gegner", {
                db.leagueByGuild(it.guild?.idLong ?: -1, it.user.idLong).handle(it.user.idLong)
            })
        }

        private suspend fun League?.handle(user: Long): Collection<String> {
            this ?: return listOf("Du bist in keiner Liga auf diesem Server!")
            leagueCache[user] = leaguename
            return nameCache.getOrPut(leaguename) {
                jda.getGuildById(guild)!!.retrieveMembersByIds(table).await()
                    .associate { it.idLong to it.user.effectiveName }
            }.values
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            val oppo = nameCache[leagueCache[user]]?.reverseGet(e.opponent) ?: return reply(
                "Gegner wurde nicht gefunden, hast du dich an die Autovervollständigung gehalten?", ephemeral = true
            )
            deferReply(true)
            handleStart(oppo)
        }
    }

    object ResultFor : CommandFeature<ResultFor.Args>(
        ::Args, CommandSpec(
            "resultfor", "Startet die interaktive Ergebniseingabe für jemand anderen",
            Constants.G.NPL, Constants.G.VIP
        )
    ) {
        init {
            restrict {
                member().hasPermission(Permission.ADMINISTRATOR) || member().unsortedRoles.any { it.idLong == 796787738559905842 }
            }
        }

        class Args : Arguments() {
            var user by member("Spieler 1", "Spieler 1")
            var opponent by member("Spieler 2", "Spieler 2")
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            deferReply(true)
            handleStart(e.opponent.idLong, userArg = e.user.idLong)
        }
    }

    object ResWithGuild : CommandFeature<ResWithGuild.Args>(
        ::Args, CommandSpec("reswithguild", "Startet die interaktive Ergebniseingabe")
    ) {
        class Args : Arguments() {
            var guild by long("guild", "guild")
            var user by long("user", "user")
            var opponent by long("opponent", "opponent")
            var tc by long("tc", "tc").nullable()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            handleStart(e.opponent, e.user, e.guild, e.tc)
        }
    }

    object ResultMenu : SelectMenuFeature<ResultMenu.Args>(::Args, SelectMenuSpec("result")) {
        class Args : Arguments() {
            var userindex by int("index", "index").compIdOnly()
            var selected by singleOption()
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<ResultEntry> {
                handleSelect(e)
            }
        }
    }

    object ResultFinish : ButtonFeature<ResultFinish.Args>(::Args, ButtonSpec("resultfinish")) {
        class Args : Arguments() {
            var mode by enumBasic<Mode>("check", "check")
        }

        enum class Mode {
            CHECK, YES, NO
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<ResultEntry> {
                handleFinish(e)
            }
        }
    }

    object ResultModal : ModalFeature<ResultModal.Args>(::Args, ModalSpec("result")) {
        class Args : Arguments() {
            var userindex by int("index", "index").compIdOnly()
            var selected by string("selected", "selected").compIdOnly()
            var kills by string<Int>("kills", "kills") {
                validate {
                    it.toIntOrNull() ?: 0
                }
                modal {
                    placeholder = "0"
                }
                default = 0
            }
            var dead by string<Boolean>("dead", "dead") {
                validate { it.equals("X", ignoreCase = true) }
                modal {
                    placeholder = "X wenn gestorben, sonst leer lassen"
                }
                default = false
            }
            var remove by string<Boolean>("remove", "remove") {
                modal(modalKey = Remove) {
                    placeholder = "X wenn ja, sonst leer lassen"
                }
                validate {
                    it.equals("X", ignoreCase = true)
                }
                default = false
            }
        }

        context(InteractionData)
        override suspend fun exec(e: Args) {
            StateStore.process<ResultEntry> {
                handleModal(e)
            }
        }
    }

    object Remove : ModalKey

    context(InteractionData)
    suspend fun handleStart(opponent: Long, userArg: Long? = null, guild: Long? = null, customTc: Long? = null) {
        val u = userArg ?: user
        val g = guild ?: gid
//        val t = customTc ?: tc
        val league = db.leagueByGuild(g, u, opponent) ?: return reply(
            "Du bist in keiner Liga mit diesem User! Wenn du denkst, dass dies ein Fehler ist, melde dich bitte bei ${Constants.MYTAG}!",
            ephemeral = true
        )
        val idx1 = league(u)
        val idx2 = league(opponent)
        val gameday = league.getGamedayData(idx1, idx2, List(2) { DraftPlayer(0, false) }).gameday
        val uuid = ResultCodesDB.add(league.leaguename, gameday, idx1, idx2)
        reply(
            "Die Ergebniseingabe wurde umgebaut und findet nun für eine effizientere Nutzung auf meiner Website statt!\nhttps://emolga.tectoast.de/result/${uuid}",
            ephemeral = true
        )
        /*ResultEntry(user, league, t).process {
            init(opponent, u)
        }*/
    }


}
