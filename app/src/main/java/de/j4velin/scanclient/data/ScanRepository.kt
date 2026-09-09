package de.j4velin.scanclient.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * The PiDMS scan server's HTTP interface.
 *
 * Two endpoints, both plain GETs: `/<pages>` opens a job for that many pages and answers as soon
 * as it is open, `/next` scans the next sheet into it. A single-page scan is just a job of one,
 * which the server completes without a `/next` - so that one request lasts as long as the scan
 * does, and is the only one [READ_TIMEOUT_MS] is really there for.
 *
 * Every reply is one human-readable line the UI shows as-is, refusals included ("a scan job is
 * already running", "no scan job has been started"), which is why [get] reads the error body.
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
                val ok = connection.responseCode in 200..299
                // On a refusal the server's explanation is on errorStream; reading inputStream
                // would throw instead, leaving the user with a bare "FileNotFoundException".
                val body = (if (ok) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }?.trim().orEmpty()
                if (ok) Result.success(body)
                else Result.failure(IOException(body.ifEmpty { "HTTP ${connection.responseCode}" }))
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
