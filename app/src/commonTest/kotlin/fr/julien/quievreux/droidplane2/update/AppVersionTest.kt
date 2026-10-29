package fr.julien.quievreux.droidplane2.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppVersionTest {

    @Test
    fun `parse normalizes leading v and metadata suffixes`() {
        val v1 = AppVersion.parse("v1.0.18")
        val v2 = AppVersion.parse("1.0.18")
        val v3 = AppVersion.parse("V1.0.18-beta.1")

        assertEquals(listOf(1, 0, 18), v1.parts)
        assertEquals(listOf(1, 0, 18), v2.parts)
        assertEquals(listOf(1, 0, 18), v3.parts)
    }

    @Test
    fun `compare versions with different segment values`() {
        val older = AppVersion.parse("1.0.18")
        val newer = AppVersion.parse("v1.0.25")

        assertTrue(newer > older)
        assertTrue(older < newer)
    }

    @Test
    fun `compare handles two-digit patch correctly without lexicographical bug`() {
        val v9 = AppVersion.parse("1.0.9")
        val v10 = AppVersion.parse("1.0.10")

        assertTrue(v10 > v9)
    }

    @Test
    fun `compare handles major and minor bumps`() {
        val v10 = AppVersion.parse("1.0.99")
        val v11 = AppVersion.parse("1.1.0")
        val v20 = AppVersion.parse("v2.0.0")

        assertTrue(v11 > v10)
        assertTrue(v20 > v11)
    }

    @Test
    fun `compare identical versions returns zero`() {
        val v1 = AppVersion.parse("v1.0.18")
        val v2 = AppVersion.parse("1.0.18")

        assertEquals(0, v1.compareTo(v2))
    }

    @Test
    fun `compare handles different segment lengths`() {
        val short = AppVersion.parse("1.0")
        val long = AppVersion.parse("1.0.1")

        assertTrue(long > short)
    }
}
