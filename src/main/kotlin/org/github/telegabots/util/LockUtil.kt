package org.github.telegabots.util

import java.util.concurrent.locks.Lock

inline fun <R> Lock.runIn(block: () -> R): R {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
