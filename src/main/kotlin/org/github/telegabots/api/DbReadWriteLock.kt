package org.github.telegabots.api

import java.util.concurrent.locks.ReadWriteLock

/**
 * [ReadWriteLock] for database access.
 */
interface DbReadWriteLock : ReadWriteLock, Service
