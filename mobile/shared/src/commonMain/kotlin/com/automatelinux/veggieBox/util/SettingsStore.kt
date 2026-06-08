package com.automatelinux.veggieBox.util

import com.russhwolf.settings.Settings

// Multiplatform persistent settings. The platform supplies a `Settings`
// (SharedPreferences on Android, NSUserDefaults on iOS), so preferences survive
// app restarts and the code is shared with a future iOS app.
class SettingsStore(private val prefs: Settings) {

    var hideDelivered: Boolean
        get() = prefs.getBoolean("hide_delivered", false)
        set(value) { prefs.putBoolean("hide_delivered", value) }
}
