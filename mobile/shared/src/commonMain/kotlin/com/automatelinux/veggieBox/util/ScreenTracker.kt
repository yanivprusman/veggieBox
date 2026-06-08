package com.automatelinux.veggieBox.util

// Tracks the currently-visible screen so a feedback report can record where the
// user was. Updated from the UI as the active tab changes (commonMain → shared
// with iOS). Read by the dev-flavor FeedbackConfig.currentScreenProvider.
object ScreenTracker {
    @Volatile
    var currentScreen: String? = null
}
