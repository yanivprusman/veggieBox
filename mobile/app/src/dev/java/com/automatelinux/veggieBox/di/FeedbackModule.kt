package com.automatelinux.veggieBox.di

import com.automatelinux.feedbacklib.FeedbackConfig
import com.automatelinux.feedbacklib.data.api.FeedbackApi
import com.automatelinux.veggieBox.BuildConfig
import com.automatelinux.veggieBox.util.ScreenTracker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// Dev-flavor only: wires the feedback-lib to the veggieBox backend. The feedback
// API lives at the same base URL the app uses (BuildConfig.API_BASE_URL).
@Module
@InstallIn(SingletonComponent::class)
object FeedbackModule {

    @Provides
    @Singleton
    fun provideFeedbackApi(): FeedbackApi {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FeedbackApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFeedbackConfig(): FeedbackConfig =
        FeedbackConfig(
            appName = "veggieBox",
            currentScreenProvider = { ScreenTracker.currentScreen },
        )
}
