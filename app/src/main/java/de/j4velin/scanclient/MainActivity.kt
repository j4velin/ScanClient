package de.j4velin.scanclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.j4velin.scanclient.ui.ScanScreen
import de.j4velin.scanclient.ui.theme.ScanClientTheme

/**
 * The whole app: one screen.
 *
 * There is no `requestedOrientation = SCREEN_ORIENTATION_PORTRAIT` any more. It was there because
 * an in-progress scan lived in two `Activity` fields and a rotation threw them away; the state is
 * in a ViewModel now and survives the configuration change on its own. API 36 ignores the lock on
 * large screens regardless, so keeping it would have bought nothing.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ScanClientTheme {
                ScanScreen()
            }
        }
    }
}
