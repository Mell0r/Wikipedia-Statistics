package ru.senin.kotlin.wiki

import com.apurebase.arkenv.Arkenv
import com.apurebase.arkenv.util.parse
import com.apurebase.arkenv.util.argument
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.lang.Integer.min
import javax.xml.parsers.SAXParserFactory
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class Parameters : Arkenv() {
    val inputs: List<File> by argument("--inputs") {
        description = "Path(s) to bzip2 archived XML file(s) with WikiMedia dump. Comma separated."
        mapping = {
            it.split(",").map{ name -> File(name) }
        }
        validate("File does not exist or cannot be read") {
            it.all { file -> file.exists() && file.isFile && file.canRead() }
        }
    }

    val output: String by argument("--output") {
        description = "Report output file"
        defaultValue = { "statistics.txt" }
    }

    val threads: Int by argument("--threads") {
        description = "Number of threads"
        defaultValue = { 4 }
        validate("Number of threads must be in 1..32") {
            it in 1..32
        }
    }
}

lateinit var parameters: Parameters

@OptIn(ExperimentalTime::class)
fun main(args: Array<String>) {
    val statistic = WikiStatistic()
    val handler = WordStatHandler(statistic)
    val factory = SAXParserFactory.newInstance()
    
    val endl = System.lineSeparator()

    fun<T> writeListInfo(outputFile: File, list: List<T>) {
        if (list.any { it != 0 }) {
            for (ind in list.indexOfFirst { it != 0 }..list.indexOfLast { it != 0 }) {
                outputFile.appendText("$ind ${list[ind]}$endl")
            }
        }
    }

    fun<T: Comparable<*>> writeMapInfo(outputFile: File, map: Map<T, Int>) {
        val sorted = map.toList().sortedWith(compareBy({ -it.second }, { it.first }))
        sorted.subList(0, min(300, sorted.size)).forEach {
            outputFile.appendText("${it.second} ${it.first}$endl")
        }
    }

    try {
        parameters = Parameters().parse(args)

        if (parameters.help) {
            println(parameters.toString())
            return
        }

        val taskManager = ParallelTaskManager(parameters.threads)
        val duration = measureTime {
            parameters.inputs.forEach {
                it.walk().forEach Inner@{ file ->
                    if (!file.isFile)
                        return@Inner

                    taskManager.submit {
                        val saxParser = factory.newSAXParser()
                        saxParser.parse(BZip2CompressorInputStream(file.inputStream().buffered()), handler)
                    }
                }
            }
            taskManager.shutdown()
            taskManager.awaitTermination()

            val outputFile = File(parameters.output)
            outputFile.writeText("")
            outputFile.appendText("Топ-300 слов в заголовках статей:$endl")
            writeMapInfo(outputFile, statistic.headerStat)

            outputFile.appendText("${endl}Топ-300 слов в статьях:$endl")
            writeMapInfo(outputFile, statistic.textStat)

            outputFile.appendText("${endl}Распределение статей по размеру:$endl")
            writeListInfo(outputFile, statistic.sizeStat)

            outputFile.appendText("${endl}Распределение статей по времени:$endl")
            writeListInfo(outputFile, statistic.yearStat)
        }
        println("Time: ${duration.toDouble(DurationUnit.MILLISECONDS)} ms")

    }
    catch (e: Exception) {
        println("Error! ${e.message}")
        throw e
    }
}