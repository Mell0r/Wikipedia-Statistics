package ru.senin.kotlin.wiki

class WikiStatistic {
    var headerStat = mutableMapOf<String, Int>()
    var textStat = mutableMapOf<String, Int>()
    var yearStat = MutableList(5000) { 0 }
    var sizeStat = MutableList(9) { 0 }

    @Synchronized
    fun updateFromCurrent(cur: CurrentWikiStatistic) {
        if (cur.headerStat == null || cur.textStat == null || cur.year == null || cur.size == null)
            return

        cur.headerStat?.forEach { (key, value) ->
            headerStat[key] = headerStat.getOrDefault(key, 0) + value
        } ?: return

        cur.textStat?.forEach { (key, value) ->
            textStat[key] = textStat.getOrDefault(key, 0) + value
        } ?: return

        sizeStat[cur.size?.minus(1) ?: return]++
        yearStat[cur.year ?: return]++
    }
}

data class CurrentWikiStatistic (
    var headerStat: MutableMap<String, Int>? = null,
    var textStat: MutableMap<String, Int>? = null,
    var size: Int? = null,
    var year: Int? = null
)