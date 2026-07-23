package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkBluePrimary,
    secondary = DarkBlueSecondary,
    tertiary = DarkBlueTertiary,
    background = DarkBlueBackground,
    surface = DarkBlueSurface,
    onPrimary = DarkBlueOnPrimary,
    onBackground = DarkBlueOnBackground,
    onSurface = DarkBlueOnSurface
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightBluePrimary,
    secondary = LightBlueSecondary,
    tertiary = LightBlueTertiary,
    background = LightBlueBackground,
    surface = LightBlueSurface,
    onPrimary = LightBlueOnPrimary,
    onBackground = LightBlueOnBackground,
    onSurface = LightBlueOnSurface
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force light blue and white colors by defaulting dynamicColor to false
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
