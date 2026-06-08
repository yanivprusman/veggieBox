package com.automatelinux.veggieBox.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.automatelinux.feedbacklib.ui.issues.FeedbackIssuesScreen
import com.automatelinux.veggieBox.BuildConfig
import com.automatelinux.veggieBox.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FeedbackIssuesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                FeedbackIssuesScreen(
                    onNavigateBack = { finish() },
                    versionName = BuildConfig.VERSION_NAME,
                )
            }
        }
    }
}
