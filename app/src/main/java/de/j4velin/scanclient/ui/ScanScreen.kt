package de.j4velin.scanclient.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.j4velin.scanclient.R
import de.j4velin.scanclient.ui.theme.ScanClientTheme

@Composable
fun ScanScreen(viewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScanScreen(
        state = state,
        onPagesInputChange = viewModel::onPagesInputChange,
        onScan = viewModel::onScan,
        onNextPage = viewModel::onNextPage,
        onIpChange = viewModel::onIpChange,
        onMessageShown = viewModel::onMessageShown,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    state: ScanUiState,
    onPagesInputChange: (String) -> Unit,
    onScan: () -> Unit,
    onNextPage: () -> Unit,
    onIpChange: (String) -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Replaces the Toasts. A Toast outlives the activity that showed it, which is why the View
    // version could pop "Error trying to scan" over whatever the user had opened next.
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onMessageShown()
    }

    Scaffold(
        // The View theme was Theme.Material.Light.DarkActionBar, so the app had a titled,
        // colour-filled bar. themes.xml is NoActionBar now because Compose owns the window, which
        // is what dropped it; this puts it back rather than leaving the screen headerless.
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PagesRow(state, onPagesInputChange, onScan)

            if (state.phase == ScanPhase.AwaitingNextPage) {
                Button(onClick = onNextPage) {
                    Text(stringResource(R.string.next_page))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(
                            R.string.page_progress, state.currentPage, state.totalPages
                        )
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            ServerIpRow(ip = state.ip, onIpChange = onIpChange)
        }
    }
}

@Composable
private fun PagesRow(
    state: ScanUiState,
    onPagesInputChange: (String) -> Unit,
    onScan: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    // The View version did this in onResume with InputMethodManager.SHOW_FORCED, deprecated since
    // API 33. Requesting focus is enough: the keyboard follows because the field is a text field.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.pages_label),
            style = MaterialTheme.typography.bodyLarge,
        )
        // Underlined and transparent rather than boxed, which is what the View EditText was:
        // an M3 OutlinedTextField draws a full border the old screen never had.
        TextField(
            value = state.pagesInput,
            onValueChange = onPagesInputChange,
            modifier = Modifier
                .width(80.dp)
                .focusRequester(focusRequester),
            enabled = state.phase == ScanPhase.Idle,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                // colorAccent, where the View theme put the focused underline and the cursor.
                focusedIndicatorColor = MaterialTheme.colorScheme.secondary,
                cursorColor = MaterialTheme.colorScheme.secondary,
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            // The Enter-key View.OnKeyListener, expressed as what it was actually for.
            keyboardActions = KeyboardActions(onDone = { if (state.canScan) onScan() }),
        )
        if (state.phase == ScanPhase.Scanning) {
            // Replaces ProgressDialog, deprecated since API 26 - and unlike the dialog it does not
            // block the whole screen for a job that only needs the buttons disabled.
            CircularProgressIndicator(Modifier.size(24.dp))
        } else {
            // colorPrimary ground, bold colorAccent label, square: the View button set all
            // three by hand, and an M3 filled button defaults to none of them.
            Button(
                onClick = onScan,
                enabled = state.canScan,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(stringResource(R.string.scan), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ServerIpRow(ip: String, onIpChange: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.server_ip_label),
            style = MaterialTheme.typography.bodyMedium,
        )
        // The View EditText was focusable="false" with a click listener - it looked like a
        // field and behaved like a button, which a TextButton's pill does not convey.
        val underline = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = ip,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clickable { editing = true }
                .width(200.dp)
                .drawBehind {
                    drawLine(
                        color = underline,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                .padding(bottom = 6.dp),
        )
    }

    if (editing) {
        IpDialog(
            current = ip,
            onDismiss = { editing = false },
            onConfirm = {
                onIpChange(it)
                editing = false
            },
        )
    }
}

@Composable
private fun IpDialog(current: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    // Seeded once from `current` and edited independently. The View dialog assigned the source
    // field's Editable straight into the dialog's EditText, so both showed one buffer and typing
    // rewrote the value whether or not the user pressed OK.
    var draft by rememberSaveable(current) { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ip_dialog_title)) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm(draft) }),
            )
        },
        confirmButton = {
            // colorAccent, which is what the platform theme tinted dialog buttons with. Left at
            // the M3 default they take the primary, which is unreadable on the dark scheme's grey.
            TextButton(
                onClick = { onConfirm(draft) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        // The View dialog had no way out but the back button, which left the OK path as the only
        // visible one.
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
            ) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ScanScreenIdlePreview() = ScanClientTheme {
    ScanScreen(ScanUiState(pagesInput = "1"), {}, {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun ScanScreenAwaitingPreview() = ScanClientTheme {
    ScanScreen(
        ScanUiState(
            pagesInput = "5",
            phase = ScanPhase.AwaitingNextPage,
            currentPage = 1,
            totalPages = 5,
        ), {}, {}, {}, {}, {}
    )
}
