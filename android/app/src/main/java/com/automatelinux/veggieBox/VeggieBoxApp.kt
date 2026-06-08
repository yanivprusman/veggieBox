package com.automatelinux.veggieBox

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class VeggieBoxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // osmdroid: set a user agent (required by OSM tile servers) and keep its
        // tile cache inside app-private storage so no storage permission is needed.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = cacheDir.resolve("osm-tiles")
        }
    }
}
