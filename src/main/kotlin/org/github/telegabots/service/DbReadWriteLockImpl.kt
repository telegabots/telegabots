package org.github.telegabots.service

import org.github.telegabots.api.DbReadWriteLock
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Implementation of [DbReadWriteLock].
 */
internal class DbReadWriteLockImpl : DbReadWriteLock {
    private val rwl: ReadWriteLock = ReentrantReadWriteLock()

    override fun readLock(): Lock  = rwl.readLock()

    override fun writeLock(): Lock  = rwl.writeLock()
}
