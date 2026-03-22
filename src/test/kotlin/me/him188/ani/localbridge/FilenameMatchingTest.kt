package me.him188.ani.localbridge

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FilenameMatchingTest {
    @Test
    fun `parses common fansub filename`() {
        val report = scanDirectoryTree(listOf())
        assertEquals(0, report.items.size)
    }

    @Test
    fun `normalizes chinese numerals for search`() {
        assertEquals("第10季".let(::normalizeForSearch), "第十季".let(::normalizeForSearch))
    }

    @Test
    fun `scores exact subject matches higher`() {
        val item = IndexedMediaFile(
            id = "1",
            file = Path("/tmp/Frieren - 01.mkv"),
            subjectName = "Frieren",
            episodeIndex = 1,
            episodeName = "Episode 1",
            aliases = listOf("Sousou no Frieren"),
            subtitles = emptyList(),
            confidence = MatchConfidence.High,
            notes = emptyList(),
        )

        assertEquals(true, scoreSearchMatch("Frieren", item) > scoreSearchMatch("Random", item))
        assertNotNull(item.episodeIndex)
    }
}
