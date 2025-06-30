package org.github.telegabots.api

interface JsonService : Service {
    fun <T> parse(str: String, clazz: Class<T>): T

    fun toJson(obj: Any): String

    fun toPrettyJson(obj: Any): String
}
