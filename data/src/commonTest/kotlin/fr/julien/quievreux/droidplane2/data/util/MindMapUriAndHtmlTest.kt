package fr.julien.quievreux.droidplane2.data.util

import fr.julien.quievreux.droidplane2.data.model.MindMapUri
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MindMapUriAndHtmlTest {

    @Test
    fun `MindMapUri parses internal link fragment`() {
        val uri = MindMapUri.parse("#ID_12345")
        assertNull(uri.scheme)
        assertEquals("ID_12345", uri.fragment)
        assertTrue(uri.isInternalLink())
    }

    @Test
    fun `MindMapUri parses external https link`() {
        val uri = MindMapUri.parse("https://example.com/docs/guide.pdf#page=2")
        assertEquals("https", uri.scheme)
        assertEquals("page=2", uri.fragment)
        assertEquals("guide.pdf", uri.lastPathSegment)
        assertFalse(uri.isInternalLink())
    }

    @Test
    fun `MindMapUri parses file path`() {
        val uri = MindMapUri.parse("file:///Users/test/maps/my_map.mm")
        assertEquals("file", uri.scheme)
        assertEquals("/Users/test/maps/my_map.mm", uri.path)
        assertEquals("my_map.mm", uri.lastPathSegment)
        assertFalse(uri.isInternalLink())
    }

    @Test
    fun `HtmlTextExtractor extracts plain text and decodes entities`() {
        val html = """
            <html>
              <head><title>Ignored</title></head>
              <body>
                <p>Hello <b>World</b> &amp; Friends!</p>
                <p>Second line &lt;test&gt;</p>
              </body>
            </html>
        """.trimIndent()
        val extracted = HtmlTextExtractor.extractPlainText(html)
        assertTrue(extracted.contains("Hello World & Friends!"))
        assertTrue(extracted.contains("Second line <test>"))
        assertFalse(extracted.contains("Ignored"))
    }
}
