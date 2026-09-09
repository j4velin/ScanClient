package de.j4velin.scanclient.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BlueGrey700,
    onPrimary = Color.White,
    primaryContainer = BlueGrey100,
    onPrimaryContainer = BlueGrey900,
    secondary = DeepOrange500,
    onSecondary = Color.White,
    secondaryContainer = DeepOrange200,
    onSecondaryContainer = DeepOrange900,
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueGrey200,
    onPrimary = BlueGrey900,
    primaryContainer = BlueGrey700,
    onPrimaryContainer = BlueGrey100,
    secondary = DeepOrange200,
    onSecondary = DeepOrange900,
    secondaryContainer = DeepOrange900,
    onSecondaryContainer = DeepOrange200,
)

/**
 * The app's Compose theme, replacing `Theme.Material.Light.DarkActionBar` in styles.xml.
 *
 * Two things the View theme could not do: it follows the system dark setting instead of being
 * light-only, and on Android 12+ it takes the wallpaper's colours. The schemes above are the
 * fallback for older devices and for anyone who turns dynamic colour off.
 */
@Composable
fun ScanClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
