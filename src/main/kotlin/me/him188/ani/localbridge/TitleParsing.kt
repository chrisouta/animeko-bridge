package me.him188.ani.localbridge

internal data class ParsedStem(
    val primaryTitle: String?,
    val otherTitles: List<String>,
    val episodeIndex: Int?,
)

internal fun parseStem(stem: String): ParsedStem {
    val episodeIndex = detectEpisodeFromRegex(stem)
    val normalized = stripReleaseTags(stem)
    val guessedTitle = guessSubjectFromStem(normalized).orEmpty()

    val titleCandidates = guessedTitle
        .split('/', '|')
        .map { it.trim() }
        .map(::cleanupTitleSegment)
        .filter { it.isNotBlank() }
        .filterNot(::looksLikeMetadataSegment)
        .distinct()

    val primary = titleCandidates.firstOrNull(::containsCjk)
        ?: titleCandidates.firstOrNull()

    return ParsedStem(
        primaryTitle = primary,
        otherTitles = titleCandidates.filterNot { it == primary },
        episodeIndex = episodeIndex,
    )
}

private fun stripReleaseTags(text: String): String {
    return text
        .replace(Regex("""^(?:\[[^\]]+]\s*)+"""), "")
        .replace(Regex("""^(?:【[^】]+】\s*)+"""), "")
        .trim()
}

private fun cleanupTitleSegment(segment: String): String {
    return segment
        .replace(Regex("""\[[^\]]*]"""), " ")
        .replace(Regex("""【[^】]*】"""), " ")
        .replace(Regex("""\([^)]+\)"""), " ")
        .replace(Regex("""[_\-.]+"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()
}

internal fun containsCjk(text: String): Boolean {
    return text.any { ch -> ch in '\u4e00'..'\u9fff' || ch in '\u3040'..'\u30ff' || ch in '\uac00'..'\ud7af' }
}

private fun looksLikeMetadataSegment(text: String): Boolean {
    val normalized = text.lowercase()
    return normalized.matches(Regex("""(?:\d{3,4}p|webrip|web dl|webdl|aac|hevc|avc|x264|x265|10bit|8bit|mp4|mkv|baha|cht|chs|简中|繁中|中字|\d+)"""))
}
