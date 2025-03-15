package org.github.telegabots.state

import org.github.telegabots.api.StateKey

/**
 * Internal user settings
 */
internal data class UserSettings(val langCode: String? = null) {

    companion object {
        val stateKey = StateKey.from(UserSettings::class.java, "INTERNAL_SETTINGS")
    }
}
