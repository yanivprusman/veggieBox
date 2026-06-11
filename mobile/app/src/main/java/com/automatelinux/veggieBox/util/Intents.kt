package com.automatelinux.veggieBox.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object Intents {
    private fun open(ctx: Context, url: String) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: Exception) {
            Toast.makeText(ctx, "לא ניתן לפתוח", Toast.LENGTH_SHORT).show()
        }
    }

    fun whatsapp(ctx: Context, waUrl: String) = open(ctx, waUrl)

    /** Open any URL in the system handler (browser / video player). */
    fun view(ctx: Context, url: String) = open(ctx, url)

    /** Open Waze navigation. By coordinates when known, else by address text. */
    fun waze(ctx: Context, lat: Double?, lon: Double?, address: String?) {
        val url = if (lat != null && lon != null) {
            "https://waze.com/ul?ll=$lat,$lon&navigate=yes"
        } else {
            "https://waze.com/ul?q=" + Uri.encode(address ?: "")
        }
        open(ctx, url)
    }

    fun dial(ctx: Context, phone: String) = open(ctx, "tel:$phone")
}
