package ru.senin.kotlin.wiki

import java.util.*
import kotlin.concurrent.thread

class ParallelTaskManager(threadPoolSize: Int) {
    private var isShutdown = false
    private val taskQueue = LinkedList<Runnable>()

    private val threadPool = Array(threadPoolSize) {
        thread(name = "Worker #$it") {
            while (true) {
                try {
                    getAvailableTask(it)?.run() ?: Thread.sleep(50)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    @Synchronized private fun getAvailableTask(index: Int): Runnable? {
        if (isShutdown && taskQueue.isEmpty()) {
            threadPool[index].interrupt()
        }

        return taskQueue.pollFirst()
    }

    fun shutdown() {
        isShutdown = true
    }

    fun awaitTermination() {
        threadPool.forEach(Thread::join)
    }

    @Synchronized fun submit(task: Runnable) {
        if (!isShutdown) {
            taskQueue.add(task)
        }
    }
}