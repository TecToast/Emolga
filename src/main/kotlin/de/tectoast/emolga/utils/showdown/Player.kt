package de.tectoast.emolga.utils.showdown

class Player(val number: Int) {
    private var spikesBy: Pokemon? = null
    private var tSpikesBy: Pokemon? = null
    private var rocksBy: Pokemon? = null
    val mons: MutableList<Pokemon> = ArrayList()
    lateinit var nickname: String
    var isWinner = false
    var totalDeaths = 0
        private set
    var teamsize = 0

    fun isInitialized() = this::nickname.isInitialized
    fun checkNickname(): String {
        return if (isInitialized()) this.nickname else "#"
    }

    fun getSpikesBy(other: Pokemon?): Pokemon? {
        return if (spikesBy == null) null else if (spikesBy!!.player === this) other else spikesBy
    }

    fun setSpikesBy(spikesBy: Pokemon?) {
        this.spikesBy = spikesBy
    }

    fun gettSpikesBy(other: Pokemon?): Pokemon? {
        return if (tSpikesBy == null) null else if (tSpikesBy!!.player === this) other else tSpikesBy
    }

    fun settSpikesBy(tSpikesBy: Pokemon?) {
        this.tSpikesBy = tSpikesBy
    }

    fun getRocksBy(other: Pokemon?): Pokemon? {
        return if (rocksBy == null) null else if (rocksBy!!.player === this) other else rocksBy
    }

    fun setRocksBy(rocksBy: Pokemon?) {
        this.rocksBy = rocksBy
    }

    fun indexOfName(s: String): Int {
        for (p in mons) {
            try {
                if (p.pokemon == s) return mons.indexOf(p)
            } catch (ignored: NullPointerException) {
            }
        }
        return -1
    }

    fun addTotalDeaths(totalDeaths: Int) {
        this.totalDeaths += totalDeaths
    }

    override fun toString(): String {
        return "Player{nickname='$nickname'}"
    }
}