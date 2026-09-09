package de.j4velin.scanclient.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.j4velin.scanclient.data.ScanRepository
import de.j4velin.scanclient.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a scan job is between "nothing running" and "all sheets through the feeder". */
enum class ScanPhase {
    /** No job open. The scan button is the only control. */
    Idle,

    /** A request is in flight; every control is disabled. */
    Scanning,

    /** A multi-page job is open and the server is waiting for the user to load the next sheet. */
    AwaitingNextPage,
}

data class ScanUiState(
    val ip: String = SettingsRepository.DEFAULT_IP,
    val pagesInput: String = "",
    val phase: ScanPhase = ScanPhase.Idle,
    /** Sheets scanned so far in the open job. */
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    /** Server reply or error, shown once in a snackbar and then cleared by [ScanViewModel.onMessageShown]. */
    val message: String? = null,
) {
    val pages: Int? get() = pagesInput.toIntOrNull()?.takeIf { it > 0 }

    /**
     * The View version read the field with `toString().toInt()` and crashed on an empty one. The
     * button is disabled instead, so there is no unparseable state to reach.
     */
    val canScan: Boolean get() = phase == ScanPhase.Idle && pages != null
}

class ScanViewModel(
    private val scanRepository: ScanRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /** Everything except the persisted IP, which the repository owns. */
    private val localState = MutableStateFlow(ScanUiState())

    val uiState: StateFlow<ScanUiState> =
        combine(localState, settingsRepository.ip) { state, ip -> state.copy(ip = ip) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScanUiState())

    fun onPagesInputChange(input: String) {
        // Digits only: the field was `numberSigned`, and a negative page count is not a thing.
        localState.update { it.copy(pagesInput = input.filter(Char::isDigit)) }
    }

    fun onIpChange(ip: String) = viewModelScope.launch {
        settingsRepository.updateIp(ip.trim())
    }

    fun onMessageShown() {
        localState.update { it.copy(message = null) }
    }

    /**
     * Opens a job for the entered number of pages.
     *
     * For a multi-page job this then scans the first sheet, which is what the View version did -
     * except that it fired both requests on separate threads at the same moment and let them race.
     * Here the second waits for the first.
     *
     * Whether that first [ScanRepository.scanNextPage] belongs here at all depends on the server:
     * it is right if `GET /<pages>` only opens the job, and one scan too many if opening a job
     * already scans the first sheet. See the note in the migration summary - deleting the marked
     * line below is the whole change if it turns out to be the latter.
     */
    fun onScan() = viewModelScope.launch {
        val state = localState.value
        val pages = state.pages ?: return@launch
        if (state.phase != ScanPhase.Idle) return@launch

        localState.update {
            it.copy(phase = ScanPhase.Scanning, totalPages = pages, currentPage = 0)
        }

        // Read through the repository rather than off uiState: uiState is a WhileSubscribed
        // stateIn, so its value is only guaranteed current while the screen is collecting it.
        val result = scanRepository.startJob(settingsRepository.ip.first(), pages)
        result.onFailure { return@launch fail(it) }
        localState.update { it.copy(message = result.getOrNull()) }

        if (pages == 1) {
            // A job of one completes on the server without a /next, as it always has.
            finishOrAwait(currentPage = 1)
        } else {
            scanPage()  // <- the first sheet of a multi-page job
        }
    }

    /** Scans the next sheet of an open job. */
    fun onNextPage() = viewModelScope.launch {
        if (localState.value.phase != ScanPhase.AwaitingNextPage) return@launch
        localState.update { it.copy(phase = ScanPhase.Scanning) }
        scanPage()
    }

    private suspend fun scanPage() {
        val result = scanRepository.scanNextPage(settingsRepository.ip.first())
        result.onFailure { return fail(it) }
        localState.update { it.copy(message = result.getOrNull()) }
        finishOrAwait(currentPage = localState.value.currentPage + 1)
    }

    private fun finishOrAwait(currentPage: Int) {
        localState.update {
            it.copy(
                currentPage = currentPage,
                phase = if (currentPage >= it.totalPages) ScanPhase.Idle else ScanPhase.AwaitingNextPage
            )
        }
    }

    /**
     * Ends the job on any failure.
     *
     * The View version left the counter where it was and re-showed the scan button only once
     * `currentPage == totalPages`, so a failed page stranded the UI with both buttons hidden.
     */
    private fun fail(cause: Throwable) {
        localState.update {
            it.copy(
                phase = ScanPhase.Idle,
                currentPage = 0,
                totalPages = 0,
                message = cause.message?.let { m -> "Scan failed: $m" } ?: "Scan failed"
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(this[APPLICATION_KEY]) as Application
                ScanViewModel(ScanRepository(), SettingsRepository(application))
            }
        }
    }
}
