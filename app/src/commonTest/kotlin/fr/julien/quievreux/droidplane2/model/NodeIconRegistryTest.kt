package fr.julien.quievreux.droidplane2.model

import fr.julien.quievreux.droidplane2.res.AppRes as R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NodeIconRegistryTest {

    @Test
    fun `resolve returns Resource for standard Freeplane drawables`() {
        val visual = NodeIconRegistry.resolve("yes")
        val res = assertIs<NodeIconVisual.Resource>(visual)
        assertEquals(R.drawable.icon_yes, res.resId)
    }

    @Test
    fun `resolve normalizes case and punctuation for button_ok and button-ok`() {
        val visual1 = NodeIconRegistry.resolve("button_ok")
        val visual2 = NodeIconRegistry.resolve("button-ok")
        val visual3 = NodeIconRegistry.resolve("Button_OK")

        val res1 = assertIs<NodeIconVisual.Resource>(visual1)
        val res2 = assertIs<NodeIconVisual.Resource>(visual2)
        val res3 = assertIs<NodeIconVisual.Resource>(visual3)
        assertEquals(R.drawable.icon_button_ok, res1.resId)
        assertEquals(R.drawable.icon_button_ok, res2.resId)
        assertEquals(R.drawable.icon_button_ok, res3.resId)
    }

    @Test
    fun `resolve maps flag-red and flag_red to red flag drawable`() {
        val visual1 = NodeIconRegistry.resolve("flag-red")
        val visual2 = NodeIconRegistry.resolve("flag_red")
        val visual3 = NodeIconRegistry.resolve("flag")

        val res1 = assertIs<NodeIconVisual.Resource>(visual1)
        val res2 = assertIs<NodeIconVisual.Resource>(visual2)
        val res3 = assertIs<NodeIconVisual.Resource>(visual3)
        assertEquals(R.drawable.icon_flag, res1.resId)
        assertEquals(R.drawable.icon_flag, res2.resId)
        assertEquals(R.drawable.icon_flag, res3.resId)
    }

    @Test
    fun `resolve maps Tabler priority vectors with aliases`() {
        val visualFull = NodeIconRegistry.resolve("full-1")
        val visualPriority = NodeIconRegistry.resolve("priority-1")
        val visualUnderscore = NodeIconRegistry.resolve("priority_1")

        assertIs<NodeIconVisual.Vector>(visualFull)
        assertIs<NodeIconVisual.Vector>(visualPriority)
        assertIs<NodeIconVisual.Vector>(visualUnderscore)
    }

    @Test
    fun `resolve returns Unmapped with exact original name for unknown icons`() {
        val unknown = "non_existent_freeplane_icon_xyz"
        val visual = NodeIconRegistry.resolve(unknown)

        val unmapped = assertIs<NodeIconVisual.Unmapped>(visual)
        assertEquals(unknown, unmapped.name)
    }

    @Test
    fun `resolve handles blank input safely`() {
        val visual = NodeIconRegistry.resolve("   ")
        assertIs<NodeIconVisual.Unmapped>(visual)
    }

    @Test
    fun `filterIcons by category returns only icons of that category`() {
        val statusIcons = NodeIconRegistry.filterIcons(category = NodeIconCategory.STATUS)
        assertTrue(statusIcons.isNotEmpty())
        assertTrue(statusIcons.all { it.category == NodeIconCategory.STATUS })

        val priorityIcons = NodeIconRegistry.filterIcons(category = NodeIconCategory.PRIORITY)
        assertTrue(priorityIcons.isNotEmpty())
        assertTrue(priorityIcons.all { it.category == NodeIconCategory.PRIORITY })
    }

    @Test
    fun `filterIcons with query filters by id, label, and alias`() {
        val flagResults = NodeIconRegistry.filterIcons(query = "flag")
        assertTrue(flagResults.size >= 7) // all colored flags

        val warningResults = NodeIconRegistry.filterIcons(query = "warning")
        assertTrue(warningResults.isNotEmpty())
    }

    @Test
    fun `getAllIcons returns complete catalog containing over 80 items`() {
        val all = NodeIconRegistry.getAllIcons()
        assertTrue(all.size >= 80, "Catalog should contain at least 80 icons, actual: ${all.size}")
    }
}
