package org.github.telegabots.state

import org.github.telegabots.api.UserService
import java.util.function.UnaryOperator

/**
 * User settings service. Manager internal [UserSettings]
 */
internal class UserSettingsService(private val userState: UserStateProvider) : UserService {
    override fun userId(): Long = userState.userId()

    fun getSettings(): UserSettings {
        return userState.get(UserSettings.stateKey, UserSettings::class.java) ?: UserSettings()
    }

    fun setSettings(settings: UserSettings) {
        userState.set(UserSettings.stateKey, settings)
    }

    fun applySettings(operator: UnaryOperator<UserSettings>) {
        setSettings(operator.apply(getSettings()))
    }
}
