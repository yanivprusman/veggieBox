package com.automatelinux.veggieBox.util

// Centralized UI strings (i18n foundation) in commonMain → shared with a future
// iOS app. Currently Hebrew; a language switch can later select another set.
// Migrate inline screen strings here over time.
object AppStrings {
    // Tabs / navigation
    const val tabRoute = "מסלול"
    const val tabMap = "מפה"
    const val tabGreeting = "חסרי פרטים"

    // Common labels
    const val hideDelivered = "הסתר משלוחים שנמסרו"
    const val delivered = "נמסר"
    const val onMyWay = "בדרך"
    const val noAddress = "— אין כתובת —"
    const val refresh = "רענן"
    const val map = "מפה"
    const val optimizeRoute = "סדר מסלול"
}
