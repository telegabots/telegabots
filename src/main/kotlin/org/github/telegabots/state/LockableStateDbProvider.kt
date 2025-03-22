package org.github.telegabots.state

import java.util.concurrent.locks.Lock

/**
 * Thread-safe version of [StateDbProvider]
 */
interface LockableStateDbProvider : StateDbProvider {
    fun readLock(): Lock

    fun writeLock(): Lock
}
