package de.tectoast.emolga.domain.game.model.analysis

enum class ReplayServerMode(val dontTranslate: Boolean) {
    LOG(false) {
        override fun getLogFromWebsiteText(text: String) = text
        override fun mapURL(url: String) = "$url.log"
    },
    SCRAPE(false) {
        private val logRegex =
            Regex("<script type=\"text/plain\" class=\"log\">(.*?)</script>", RegexOption.DOT_MATCHES_ALL)

        override fun getLogFromWebsiteText(text: String) = logRegex.find(text)?.groupValues[1] ?: ""
    },
    POKEATHLON(true) {
        override fun getLogFromWebsiteText(text: String) = text

        override fun mapURL(url: String) = "https://sim.pokeathlon.com/replays/${url.substringAfterLast("=")}.log"
    };

    abstract fun getLogFromWebsiteText(text: String): String
    open fun mapURL(url: String) = url
}