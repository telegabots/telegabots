package org.github.telegabots.api

data class InputUser(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val userName: String,
    val isBot: Boolean
)
