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
    background = Grey50,
    onBackground = Grey900,
    surface = Grey50,
    onSurface = Grey900,
    surfaceVariant = Grey200,
    onSurfaceVariant = Grey600,
    // Dialogs. M3 floats them on a tinted surfaceContainerHigh; the platform ones were plain white.
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
)

// Same blue grey and same orange as the light one, on Material's dark greys rather than M3's
// near-black. The app's two colours are what makes it recognisable; only the ground changes.
private val DarkColorScheme = darkColorScheme(
    primary = BlueGrey700,
    onPrimary = Color.White,
    primaryContainer = BlueGrey700,
    onPrimaryContainer = BlueGrey100,
    secondary = DeepOrange500,
    onSecondary = Color.White,
    secondaryContainer = DeepOrange900,
    onSecondaryContainer = DeepOrange200,
    background = Grey850,
    onBackground = Grey50,
    surface = Grey850,
    onSurface = Grey50,
    surfaceVariant = Grey800,
    onSurfaceVariant = Grey400,
    surfaceContainer = Grey800,
    surfaceContainerHigh = Grey800,
)

/**
 * The app's Compose theme, replacing `Theme.Material.Light.DarkActionBar` in styles.xml.
 *
 * The schemes above are the app's own, built from the View theme's #455A64 / #FF5722, and they
 * are what the app uses: [dynamicColor] defaults to off, because taking the wallpaper's colours
 * on Android 12+ meant the app no longer looked like itself.
 *
 * The one thing this does that the light-only View theme could not is follow the system dark
 * setting. Both schemes are built from the same two colours and differ only in their neutrals, so
 * the dark one is the same app on a dark grey ground rather than a different-looking app.
 */
@Composable
fun ScanClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
