package org.github.telegabots.api

enum class TaskState {
    /**
     * Task registered but not started yet
     */
    Initted,

    /**
     * Task is starting
     */
    Starting,

    /**
     * Task finally started
     */
    Started,

    /**
     * Task is stopping
     */
    Stopping,

    /**
     * Task stopped finally
     */
    Stopped
}
