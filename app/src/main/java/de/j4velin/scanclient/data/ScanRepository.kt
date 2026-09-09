package de.j4velin.scanclient.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * The PiDMS scan server's HTTP interface.
 *
 * Two endpoints, both plain GETs returning a human-readable status line that the UI shows as-is:
 * `/<pages>` opens a job for that many pages, `/next` scans the next sheet into the open job. A
 * single-page scan is just a job of one, which the server completes without a `/next`.
 */
class ScanRepository {

    /** Opens a scan job for [pages] sheets. */
    suspend fun startJob(ip: String, pages: Int): Result<String> = get(ip, pages.toString())

    /** Scans the next sheet into the job opened by [startJob]. */
    suspend fun scanNextPage(ip: String): Result<String> = get(ip, "next")

    /**
     * The server is a Pi on the local network and answers in well under a second, but nothing
     * guarantees it is switched on. `URL.readText()`, which this replaces, applied no timeout at
     * all: a request to an IP that does not answer hung until the platform's own socket timeout,
     * with no way to cancel it because it ran on a bare [Thread]. These two keep it bounded, and
     * running inside a coroutine means the caller's scope can drop the result.
     */
    private suspend fun get(ip: String, path: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val connection = (URL("http://$ip:$PORT/$path").openConnection() as HttpURLConnection)
                .apply {
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                }
            try {
                Result.success(connection.inputStream.bufferedReader().use { it.readText() }.trim())
            } finally {
                connection.disconnect()
            }
        } catch (e: CancellationException) {
            // Cancellation is not a scan failure - let it propagate rather than reporting it as one.
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val PORT = 8080
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 60_000
    }
}
