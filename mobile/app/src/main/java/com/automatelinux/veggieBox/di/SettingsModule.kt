package com.automatelinux.veggieBox.di

import android.content.Context
import com.automatelinux.veggieBox.util.SettingsStore
import com.russhwolf.settings.SharedPreferencesSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {

    @Provides
    @Singleton
    fun provideSettingsStore(@ApplicationContext ctx: Context): SettingsStore {
        val prefs = ctx.getSharedPreferences("veggiebox_settings", Context.MODE_PRIVATE)
        return SettingsStore(SharedPreferencesSettings(prefs))
    }
}
