package app.multiwidgets.ai

import app.multiwidgets.ai.subtitles.Segment
import app.multiwidgets.ai.subtitles.SubtitleBus
import org.junit.Assert.assertEquals
import org.junit.Test

/** La file de sous-titres reste bornée : jamais toute une conversation en RAM. */
class SubtitleBusTest {

    @Test fun `la file garde au plus douze segments`() {
        SubtitleBus.vider()
        repeat(30) { i -> SubtitleBus.publier(Segment("s$i", "en", "t$i")) }
        assertEquals(12, SubtitleBus.segments.value.size)
        assertEquals("t29", SubtitleBus.segments.value.last().traduction)
        assertEquals("t18", SubtitleBus.segments.value.first().traduction)
    }

    @Test fun `vider repart proprement`() {
        SubtitleBus.vider()
        assertEquals(0, SubtitleBus.segments.value.size)
    }
}
