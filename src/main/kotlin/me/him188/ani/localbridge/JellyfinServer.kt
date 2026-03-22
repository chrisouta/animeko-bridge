package me.him188.ani.localbridge

import com.sun.net.httpserver.Headers
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.URLDecoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

class LocalJellyfinServer(
    private val settings: () -> ServerSettings,
    private val items: () -> List<IndexedMediaFile>,
) : Closeable {
    private val json = Json { prettyPrint = false; encodeDefaults = true }
    private var server: HttpServer? = null

    fun start() {
        if (server != null) return
        val config = settings()
        val httpServer = HttpServer.create(InetSocketAddress(config.bindMode.host, config.port), 0)
        httpServer.executor = Executors.newCachedThreadPool()
        httpServer.createContext("/health") { exchange ->
            respondText(exchange, 200, "ok")
        }
        httpServer.createContext("/Items") { exchange ->
            handleItems(exchange)
        }
        httpServer.createContext("/Items/") { exchange ->
            handleDownload(exchange)
        }
        httpServer.createContext("/Videos/") { exchange ->
            handleSubtitle(exchange)
        }
        httpServer.start()
        server = httpServer
    }

    fun stop() {
        server?.stop(0)
        server = null
    }

    val isRunning: Boolean
        get() = server != null

    override fun close() {
        stop()
    }

    fun visibleUrls(): List<String> {
        val config = settings()
        return buildList {
            add("http://127.0.0.1:${config.port}")
            if (config.bindMode == BindMode.Lan) {
                NetworkInterface.getNetworkInterfaces().toList()
                    .filterNot { it.isLoopback || !it.isUp }
                    .flatMap { it.inetAddresses.toList() }
                    .map { it.hostAddress }
                    .filter { it.contains(':').not() }
                    .distinct()
                    .forEach { add("http://$it:${config.port}") }
            }
        }
    }

    private fun handleItems(exchange: HttpExchange) {
        if (!authorize(exchange, allowQueryApiKey = false)) return
        val query = parseQuery(exchange.requestURI.rawQuery)
        val searchTerm = query["searchTerm"].orEmpty().trim()
        val all = items()
        val filtered = if (searchTerm.isBlank()) {
            all.filter { it.episodeIndex != null }.take(200)
        } else {
            all.asSequence()
                .map { it to scoreSearchMatch(searchTerm, it) }
                .filter { (_, score) -> score >= 50 }
                .sortedByDescending { it.second }
                .map { it.first }
                .filter { it.episodeIndex != null }
                .take(200)
                .toList()
        }

        val payload = SearchResponse(
            Items = filtered.map { item ->
                SearchItem(
                    Name = item.episodeName,
                    SeasonName = item.subjectName,
                    SeriesName = item.subjectName,
                    Id = item.id,
                    IndexNumber = item.episodeIndex,
                    Type = "Episode",
                    MediaStreams = item.subtitles.mapIndexed { index, subtitle ->
                        MediaStream(
                            Title = subtitle.title,
                            Language = subtitle.language,
                            Type = "Subtitle",
                            Codec = subtitle.codec,
                            Index = index,
                            IsExternal = true,
                            IsTextSubtitleStream = true,
                        )
                    },
                )
            },
        )
        respondJson(exchange, 200, payload)
    }

    private fun handleDownload(exchange: HttpExchange) {
        if (!authorize(exchange, allowQueryApiKey = true)) return
        val path = exchange.requestURI.path.removePrefix("/Items/").substringBefore("/Download")
        val item = items().firstOrNull { it.id == path }
        if (item == null) {
            respondText(exchange, 404, "Not found")
            return
        }
        serveFile(exchange, item.file)
    }

    private fun handleSubtitle(exchange: HttpExchange) {
        if (!authorize(exchange, allowQueryApiKey = true)) return
        val parts = exchange.requestURI.path.removePrefix("/Videos/").split('/')
        if (parts.size < 5) {
            respondText(exchange, 404, "Not found")
            return
        }
        val itemId = parts[0]
        val subtitleIndex = parts.getOrNull(3)?.toIntOrNull()
        val item = items().firstOrNull { it.id == itemId }
        val subtitle = subtitleIndex?.let { index -> item?.subtitles?.getOrNull(index) }
        if (subtitle == null) {
            respondText(exchange, 404, "Not found")
            return
        }
        serveFile(exchange, subtitle.path)
    }

    private fun authorize(exchange: HttpExchange, allowQueryApiKey: Boolean): Boolean {
        val config = settings()
        val query = parseQuery(exchange.requestURI.rawQuery)
        val headerToken = exchange.requestHeaders["Authorization"]
            ?.firstOrNull()
            ?.substringAfter("Token=\"", "")
            ?.substringBefore('"')
        val apiKey = if (allowQueryApiKey) query["ApiKey"] else null
        val queryUserId = query["userId"]

        val tokenOk = listOfNotNull(headerToken, apiKey).any { it == config.apiKey }
        val userOk = queryUserId == null || queryUserId == config.userId
        if (!tokenOk || !userOk) {
            respondText(exchange, 401, "Unauthorized")
            return false
        }
        return true
    }

    private fun serveFile(exchange: HttpExchange, file: Path) {
        val fileSize = Files.size(file)
        val rangeHeader = exchange.requestHeaders.getFirst("Range")
        val (start, end, status) = parseRange(rangeHeader, fileSize)
        val contentLength = end - start + 1
        val method = exchange.requestMethod.uppercase()

        val contentType = Files.probeContentType(file)
            ?: when (file.toString().substringAfterLast('.', "").lowercase()) {
                "mkv" -> "video/x-matroska"
                "mp4", "m4v" -> "video/mp4"
                "ts" -> "video/mp2t"
                "webm" -> "video/webm"
                "ass", "ssa" -> "text/x-ass"
                "srt" -> "application/x-subrip"
                "vtt" -> "text/vtt"
                else -> "application/octet-stream"
            }

        exchange.responseHeaders.apply {
            add("Accept-Ranges", "bytes")
            add("Content-Type", contentType)
            add("Content-Length", contentLength.toString())
            if (status == 206) {
                add("Content-Range", "bytes $start-$end/$fileSize")
            }
        }

        if (method == "HEAD") {
            exchange.sendResponseHeaders(status, -1)
            exchange.close()
            return
        }

        exchange.sendResponseHeaders(status, contentLength)
        Files.newInputStream(file).buffered().use { input ->
            exchange.responseBody.use { output ->
                input.skip(start)
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var remaining = contentLength
                while (remaining > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    remaining -= read
                }
            }
        }
    }

    private fun parseRange(rangeHeader: String?, fileSize: Long): Triple<Long, Long, Int> {
        if (rangeHeader == null || !rangeHeader.startsWith("bytes=")) {
            return Triple(0L, fileSize - 1, 200)
        }
        val range = rangeHeader.removePrefix("bytes=").substringBefore(',')
        val start = range.substringBefore('-').toLongOrNull() ?: 0L
        val end = range.substringAfter('-', "").toLongOrNull() ?: (fileSize - 1)
        return Triple(start.coerceAtLeast(0), end.coerceAtMost(fileSize - 1), 206)
    }

    private fun parseQuery(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&')
            .mapNotNull { part ->
                val key = part.substringBefore('=', "")
                if (key.isBlank()) return@mapNotNull null
                val value = part.substringAfter('=', "")
                key to URLDecoder.decode(value, Charsets.UTF_8)
            }
            .toMap()
    }

    private fun respondJson(exchange: HttpExchange, status: Int, payload: Any) {
        val body = when (payload) {
            is SearchResponse -> json.encodeToString(payload)
            else -> error("Unsupported payload")
        }.toByteArray()
        exchange.responseHeaders.add("Content-Type", "application/json; charset=utf-8")
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.use { it.write(body) }
    }

    private fun respondText(exchange: HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray()
        exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}

@Serializable
private data class SearchResponse(
    val Items: List<SearchItem> = emptyList(),
)

@Serializable
private data class SearchItem(
    val Name: String,
    val SeasonName: String? = null,
    val SeriesName: String? = null,
    val Id: String,
    val IndexNumber: Int? = null,
    val ParentIndexNumber: Int? = null,
    val Type: String,
    val MediaStreams: List<MediaStream> = emptyList(),
)

@Serializable
private data class MediaStream(
    val Title: String? = null,
    val Language: String? = null,
    val Type: String,
    val Codec: String? = null,
    val Index: Int,
    val IsExternal: Boolean,
    val IsTextSubtitleStream: Boolean,
)

private fun <T> java.util.Enumeration<T>.toList(): List<T> = buildList {
    while (hasMoreElements()) {
        add(nextElement())
    }
}
