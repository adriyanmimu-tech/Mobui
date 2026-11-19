package com.example

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import com.example.data.RangeContinuableHttpDataSource
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class RangeContinuableHttpDataSourceTest {

    @Test
    fun serverCapped206Chunks_fullReadAndSeekDeliverExactBytes() {
        val totalSize = 131_072 // 128 KB synthetic MP4 payload
        val payload = ByteArray(totalSize) { i -> ((i * 31 + 17) and 0xFF).toByte() }
        val requestCounter = AtomicInteger(0)

        val server = ServerSocket(0, 16, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val running = AtomicBoolean(true)

        val serverThread = thread(isDaemon = true) {
            while (running.get()) {
                val socket = try {
                    server.accept()
                } catch (_: Exception) {
                    break
                }
                try {
                    socket.use { s ->
                        val reader = BufferedReader(InputStreamReader(s.getInputStream()))
                        val requestLine = reader.readLine() ?: return@use
                        var rangeHeader: String? = null
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                            if (line.startsWith("Range:", ignoreCase = true)) {
                                rangeHeader = line.substringAfter(":").trim()
                            }
                        }

                        val reqIdx = requestCounter.incrementAndGet()
                        val out = s.getOutputStream()

                        if (requestLine.contains("/full200")) {
                            val header = buildString {
                                append("HTTP/1.1 200 OK\r\n")
                                append("Content-Type: application/vnd.apple.mpegurl\r\n")
                                append("Content-Length: ${payload.size}\r\n")
                                append("Connection: close\r\n\r\n")
                            }
                            out.write(header.toByteArray())
                            out.write(payload)
                            out.flush()
                            return@use
                        }

                        // Mimic hd-w0l1.onrender.com/api/mb-stream.mp4:
                        // Cap chunk 0 at 16 KB and subsequent chunks at 32 KB via HTTP 206 Partial Content!
                        var start = 0
                        var reqEnd = totalSize - 1
                        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                            val parts = rangeHeader.removePrefix("bytes=").split("-")
                            start = parts.getOrNull(0)?.toIntOrNull() ?: 0
                            reqEnd = parts.getOrNull(1)?.toIntOrNull() ?: (totalSize - 1)
                        }

                        if (start >= totalSize) {
                            out.write("HTTP/1.1 416 Range Not Satisfiable\r\nContent-Range: bytes */$totalSize\r\nConnection: close\r\n\r\n".toByteArray())
                            out.flush()
                            return@use
                        }

                        val cap = if (start == 0) 16_384 else 32_768
                        val end = minOf(reqEnd, start + cap - 1, totalSize - 1)
                        val chunkLen = end - start + 1

                        val header = buildString {
                            append("HTTP/1.1 206 Partial Content\r\n")
                            append("Content-Type: video/mp4\r\n")
                            append("Accept-Ranges: bytes\r\n")
                            append("Content-Length: $chunkLen\r\n")
                            append("Content-Range: bytes $start-$end/$totalSize\r\n")
                            append("X-Req-Index: $reqIdx\r\n")
                            append("Connection: close\r\n\r\n")
                        }
                        out.write(header.toByteArray())
                        out.write(payload, start, chunkLen)
                        out.flush()
                    }
                } catch (_: Exception) {
                    // Ignore test socket teardown
                }
            }
        }

        try {
            val factory = RangeContinuableHttpDataSource.Factory(
                headers = mapOf("X-Test-Header" to "muvio"),
                connectTimeoutMs = 5_000,
                readTimeoutMs = 5_000
            )

            // 1. Unbounded full read from 0 across 5 server-capped HTTP 206 chunks (16K + 32K + 32K + 32K + 16K)
            val ds1 = factory.createDataSource()
            val fullUri = Uri.parse("http://127.0.0.1:$port/api/mb-stream.mp4")
            val reportedLen = ds1.open(DataSpec(fullUri))
            assertEquals("Must report total file size from Content-Range, not 16 KB chunk size", totalSize.toLong(), reportedLen)
            val fullRead = readAll(ds1)
            ds1.close()
            assertEquals(totalSize, fullRead.size)
            assertArrayEquals(payload, fullRead)
            assertTrue("Should have issued multiple Range continuation requests", requestCounter.get() >= 4)

            // 2. Mid-stream seek (position = 40_000, length = UNSET)
            val ds2 = factory.createDataSource()
            val seekPos = 40_000L
            val seekLen = ds2.open(DataSpec.Builder().setUri(fullUri).setPosition(seekPos).build())
            assertEquals(totalSize - seekPos, seekLen)
            val seekRead = readAll(ds2)
            ds2.close()
            assertArrayEquals(payload.copyOfRange(seekPos.toInt(), totalSize), seekRead)

            // 3. Bounded range spanning multiple server-capped chunks (10_000 .. 59_999)
            val ds3 = factory.createDataSource()
            val boundedLen = ds3.open(
                DataSpec.Builder().setUri(fullUri).setPosition(10_000L).setLength(50_000L).build()
            )
            assertEquals(50_000L, boundedLen)
            val boundedRead = readAll(ds3)
            ds3.close()
            assertArrayEquals(payload.copyOfRange(10_000, 60_000), boundedRead)

            // 4. Standard HTTP 200 OK (HLS playlist / subtitle) in 1 single request
            val before200 = requestCounter.get()
            val ds4 = factory.createDataSource()
            val okUri = Uri.parse("http://127.0.0.1:$port/full200")
            val okLen = ds4.open(DataSpec(okUri))
            assertEquals(totalSize.toLong(), okLen)
            val okRead = readAll(ds4)
            ds4.close()
            assertArrayEquals(payload, okRead)
            assertEquals("Standard 200 OK must complete in 1 request", before200 + 1, requestCounter.get())
        } finally {
            running.set(false)
            runCatching { server.close() }
            serverThread.join(1_000)
        }
    }

    private fun readAll(ds: androidx.media3.datasource.DataSource): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(4_096)
        while (true) {
            val n = ds.read(buf, 0, buf.size)
            if (n == C.RESULT_END_OF_INPUT) break
            if (n > 0) out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
