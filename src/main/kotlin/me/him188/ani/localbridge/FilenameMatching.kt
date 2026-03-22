package me.him188.ani.localbridge

import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

private val mediaExtensions = setOf("mkv", "mp4", "avi", "mov", "m4v", "ts", "m2ts", "webm")
private val subtitleExtensions = setOf("ass", "ssa", "srt", "vtt")
private val seasonFolderRegex = Regex("""^(season|s)\s*\d+$""", RegexOption.IGNORE_CASE)
private val explicitEpisodeRegexes = listOf(
    Regex("""(?i)\bS\d{1,2}E(\d{1,3})\b"""),
    Regex("""(?i)\bEP?\.?\s*(\d{1,3})\b"""),
    Regex("""第\s*(\d{1,3})\s*[话話集]\b"""),
    Regex("""(?<!\d)(\d{1,3})(?!\d)"""),
)

fun Path.isMediaFile(): Boolean = extension().lowercase() in mediaExtensions

fun Path.isSubtitleFile(): Boolean = extension().lowercase() in subtitleExtensions

fun Path.isSupportedLibraryFile(): Boolean = isMediaFile() || isSubtitleFile()

fun scanDirectoryTree(roots: List<Path>): ScanReport {
    val mediaFiles = mutableListOf<Path>()
    val subtitleFiles = mutableListOf<Path>()
    val skipped = mutableListOf<Path>()

    roots.distinct().forEach { root ->
        java.nio.file.Files.walk(root).use { stream ->
            stream.filter { Files.isRegularFile(it) }.forEach { path ->
                when {
                    path.isMediaFile() -> mediaFiles.add(path)
                    path.isSubtitleFile() -> subtitleFiles.add(path)
                    else -> skipped.add(path)
                }
            }
        }
    }

    val subtitlesByStem = subtitleFiles.groupBy { subtitleStemKey(it) }
    val items = mediaFiles.sortedBy { it.toString().lowercase() }.map { media ->
        parseIndexedMediaFile(media, subtitlesByStem[subtitleStemKey(media)].orEmpty())
    }

    return ScanReport(roots, items, skipped)
}

private fun subtitleStemKey(path: Path): String {
    return path.parent?.resolve(path.nameWithoutExtension())?.toString().orEmpty().lowercase()
}

private fun parseIndexedMediaFile(
    file: Path,
    sidecarSubtitles: List<Path>,
): IndexedMediaFile {
    val stem = file.nameWithoutExtension()
    val parsedStem = parseStem(stem)
    val folderCandidates = folderCandidates(file)
    val aliasCandidates = buildList {
        parsedStem.primaryTitle?.let(::add)
        addAll(parsedStem.otherTitles)
        addAll(folderCandidates)
        guessSubjectFromStem(stem)?.let(::add)
    }.map(String::trim).filter(String::isNotBlank).distinct()

    val episodeIndex = parsedStem.episodeIndex ?: detectEpisodeFromRegex(stem)
    val subjectName = aliasCandidates.firstOrNull().orEmpty()
        .ifBlank { folderCandidates.firstOrNull().orEmpty() }
        .ifBlank { stem }
    val episodeName = buildEpisodeName(stem, episodeIndex)
    val notes = mutableListOf<ScanNote>()

    if (parsedStem.primaryTitle != null || parsedStem.otherTitles.isNotEmpty()) {
        notes += ScanNote.ParsedTitleFromFilename
    } else if (folderCandidates.isNotEmpty()) {
        notes += ScanNote.UsedParentFolderAsTitle
    } else {
        notes += ScanNote.UsedRawFilenameAsTitle
    }

    if (episodeIndex != null) {
        notes += ScanNote.DetectedEpisode(episodeIndex)
    } else {
        notes += ScanNote.EpisodeNeedsReview
    }

    val confidence = when {
        parsedStem.primaryTitle != null && containsCjk(parsedStem.primaryTitle) && episodeIndex != null -> MatchConfidence.High
        parsedStem.otherTitles.isNotEmpty() && episodeIndex != null -> MatchConfidence.High
        folderCandidates.isNotEmpty() && episodeIndex != null -> MatchConfidence.Medium
        episodeIndex != null -> MatchConfidence.Low
        else -> MatchConfidence.Unknown
    }

    return IndexedMediaFile(
        id = UUID.randomUUID().toString(),
        file = file,
        subjectName = subjectName,
        episodeIndex = episodeIndex,
        episodeName = episodeName,
        aliases = aliasCandidates,
        subtitles = sidecarSubtitles.mapIndexed { index, subtitle ->
            IndexedSubtitle(
                id = "${file.toAbsolutePath()}#subtitle#$index",
                path = subtitle,
                codec = subtitle.extension().lowercase(),
                title = subtitle.fileName.toString(),
            )
        },
        confidence = confidence,
        notes = notes,
    )
}

private fun folderCandidates(file: Path): List<String> {
    val candidates = generateSequence(file.parent) { it.parent }
        .take(3)
        .map { it.fileName?.toString().orEmpty() }
        .filter { it.isNotBlank() }
        .filterNot { seasonFolderRegex.matches(it) }
        .toList()
    return candidates
}

internal fun guessSubjectFromStem(stem: String): String? {
    val cleaned = explicitEpisodeRegexes.fold(stem) { acc, regex ->
        acc.replace(regex, " ")
    }
    return cleaned
        .replace(Regex("""\[[^\]]*]"""), " ")
        .replace(Regex("""\([^)]+\)"""), " ")
        .replace(Regex("""\b(1080p|720p|2160p|webrip|web-dl|aac|hevc|avc|x264|x265|10bit|8bit|简繁|简中|繁中|中字)\b""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""[_\-.]+"""), " ")
        .trim()
        .takeIf { it.isNotBlank() }
}

internal fun detectEpisodeFromRegex(stem: String): Int? {
    explicitEpisodeRegexes.forEach { regex ->
        regex.find(stem)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            if (it > 0) return it
        }
    }
    return null
}

private fun buildEpisodeName(stem: String, episodeIndex: Int?): String {
    val stripped = guessSubjectFromStem(stem)?.trim().orEmpty()
    if (stripped.isNotBlank() && stripped.length < stem.length) {
        return stripped
    }
    return if (episodeIndex != null) "第${episodeIndex}话" else stem
}

fun normalizeForSearch(text: String): String {
    return buildString(text.length) {
        text.lowercase().forEach { ch ->
            when {
                ch in listOf('一', '二', '三', '四', '五', '六', '七', '八', '九', '十') -> append(
                    when (ch) {
                        '一' -> '1'
                        '二' -> '2'
                        '三' -> '3'
                        '四' -> '4'
                        '五' -> '5'
                        '六' -> '6'
                        '七' -> '7'
                        '八' -> '8'
                        '九' -> '9'
                        else -> "10"
                    }
                )
                ch.isLetterOrDigit() -> append(ch)
                else -> Unit
            }
        }
    }
}

fun scoreSearchMatch(query: String, item: IndexedMediaFile): Int {
    val normalizedQuery = normalizeForSearch(query)
    if (normalizedQuery.isBlank()) return 0

    return item.searchableTitles.maxOf { candidate ->
        val normalizedCandidate = normalizeForSearch(candidate)
        when {
            normalizedCandidate == normalizedQuery -> 100
            normalizedCandidate.contains(normalizedQuery) -> 90
            normalizedQuery.contains(normalizedCandidate) -> 85
            else -> simpleSimilarity(normalizedCandidate, normalizedQuery)
        }
    }
}

private fun simpleSimilarity(a: String, b: String): Int {
    if (a.isBlank() || b.isBlank()) return 0
    val common = a.toSet().intersect(b.toSet()).size
    return (common * 100 / maxOf(a.length, b.length)).coerceIn(0, 100)
}
private fun Path.extension(): String {
    val name = fileName.toString()
    return name.substringAfterLast('.', "")
}

private fun Path.nameWithoutExtension(): String {
    val name = fileName.toString()
    return if ('.' in name) name.substringBeforeLast('.') else name
}
