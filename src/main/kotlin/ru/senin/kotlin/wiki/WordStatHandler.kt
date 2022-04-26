package ru.senin.kotlin.wiki

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

class WordStatHandler(private val stat: WikiStatistic) : DefaultHandler() {
    private var pageDepth = -1
    private var revisionDepth = -1
    private var lastEntity = StringBuilder()

    var curDepth = 0

    var cur = CurrentWikiStatistic()

    override fun startElement(
        uri: String,
        localName: String,
        qName: String,
        attributes: Attributes
    ) {
        lastEntity = StringBuilder()

        curDepth++

        when (qName) {
            "page" -> {
                pageDepth = curDepth
                cur = CurrentWikiStatistic()
            }

            "revision" -> revisionDepth = curDepth

            "text" -> if (pageDepth == curDepth - 2 && revisionDepth == curDepth - 1) {
                cur.size = attributes.getValue("bytes")?.length
            }
        }
    }

    private fun parseLastEntity() : Sequence<MatchResult> =
        Regex("[а-яА-Я]{3,}").findAll(lastEntity)

    override fun endElement(
        uri: String,
        localName: String,
        qName: String
    ) {
        when (qName) {
            "title" -> if (pageDepth == curDepth - 1) {
                cur.headerStat = parseLastEntity().groupBy { it.value.lowercase() }
                    .mapValues { it.value.size }.toMutableMap()
            }

            "text" -> if (pageDepth == curDepth - 2 && revisionDepth == curDepth - 1) {
                cur.textStat = parseLastEntity().groupBy { it.value.lowercase() }
                    .mapValues { it.value.size }.toMutableMap()
            }

            "timestamp" -> if (pageDepth == curDepth - 2 && revisionDepth == curDepth - 1) {
                cur.year = lastEntity.substring(0, lastEntity.indexOfFirst { it == '-' }).toIntOrNull()
            }

            "revision" -> revisionDepth = -1

            "page" -> {
                pageDepth = -1

                stat.updateFromCurrent(cur)
            }
        }

        curDepth--
    }

    override fun characters(entity: CharArray, start: Int, length: Int) {
        lastEntity.append(entity, start, length)
    }
}