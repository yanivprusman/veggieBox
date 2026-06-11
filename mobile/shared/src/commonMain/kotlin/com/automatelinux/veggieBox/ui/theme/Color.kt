package com.automatelinux.veggieBox.ui.theme

import androidx.compose.ui.graphics.Color

// Veggie-green Material 3 palette (seed ≈ #2E7D32), light + dark.
// Roles, not raw hex, are what screens should use:
//   primary            = green (delivered / confirm actions)
//   tertiary           = carrot orange (pending / attention / house instructions)
//   secondary          = muted green-gray (neutral accents, profit chip)
//   *Container roles   = tinted card/sheet backgrounds with guaranteed-contrast "on" colors

// Brand constant: the top app bar stays this green in BOTH modes — it's the app's
// identity (matches the web header's green-700).
val BrandGreen = Color(0xFF2E7D32)

// ---- Light scheme ----
val LightPrimary = Color(0xFF2E7D32)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB6F2AF)
val LightOnPrimaryContainer = Color(0xFF002204)
val LightSecondary = Color(0xFF52634F)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD5E8CF)
val LightOnSecondaryContainer = Color(0xFF101F10)
val LightTertiary = Color(0xFFA15200)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDCC2)
val LightOnTertiaryContainer = Color(0xFF341100)
val LightBackground = Color(0xFFF7FBF1)
val LightOnBackground = Color(0xFF191D17)
val LightSurface = Color(0xFFFCFEF6)
val LightOnSurface = Color(0xFF191D17)
val LightSurfaceVariant = Color(0xFFDEE5D8)
val LightOnSurfaceVariant = Color(0xFF424940)
val LightOutline = Color(0xFF72796F)
val LightOutlineVariant = Color(0xFFC2C9BD)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

// ---- Dark scheme ----
val DarkPrimary = Color(0xFF9BD78F)
val DarkOnPrimary = Color(0xFF0A390F)
val DarkPrimaryContainer = Color(0xFF255427)
val DarkOnPrimaryContainer = Color(0xFFB7F2AC)
val DarkSecondary = Color(0xFFBACCB3)
val DarkOnSecondary = Color(0xFF253423)
val DarkSecondaryContainer = Color(0xFF3B4B38)
val DarkOnSecondaryContainer = Color(0xFFD5E8CF)
val DarkTertiary = Color(0xFFFFB77C)
val DarkOnTertiary = Color(0xFF562F00)
val DarkTertiaryContainer = Color(0xFF7A4510)
val DarkOnTertiaryContainer = Color(0xFFFFDCC2)
val DarkBackground = Color(0xFF101410)
val DarkOnBackground = Color(0xFFE0E4DB)
val DarkSurface = Color(0xFF171B16)
val DarkOnSurface = Color(0xFFE0E4DB)
val DarkSurfaceVariant = Color(0xFF424940)
val DarkOnSurfaceVariant = Color(0xFFC2C9BD)
val DarkOutline = Color(0xFF8C9388)
val DarkOutlineVariant = Color(0xFF424940)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
