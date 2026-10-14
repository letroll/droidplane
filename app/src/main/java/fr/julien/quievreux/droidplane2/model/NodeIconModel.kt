package fr.julien.quievreux.droidplane2.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import fr.julien.quievreux.droidplane2.R

/**
 * Visual representation of an icon for Jetpack Compose rendering.
 */
sealed interface NodeIconVisual {
    /** Renders via androidx.compose.material3.Icon with ImageVector */
    data class Vector(val imageVector: ImageVector) : NodeIconVisual

    /** Renders via androidx.compose.ui.res.painterResource(@DrawableRes resId) */
    data class Resource(@DrawableRes val resId: Int) : NodeIconVisual

    /** Fallback rendering: a compact textual badge displaying [name] */
    data class Unmapped(val name: String) : NodeIconVisual
}

/**
 * Thematic categories for grouping and browsing Freeplane icons in the inspector catalog.
 */
enum class NodeIconCategory(@StringRes val titleRes: Int) {
    ALL(R.string.category_all),
    STATUS(R.string.category_status),
    PRIORITY(R.string.category_priority),
    EMOTION(R.string.category_emotion),
    ACTIONS_TOOLS(R.string.category_actions_tools),
    PEOPLE_OBJECTS(R.string.category_people_objects)
}

/**
 * Metadata describing an available Freeplane icon in the catalog.
 */
data class NodeIconDescriptor(
    val id: String,
    val visual: NodeIconVisual,
    val label: String,
    val category: NodeIconCategory,
    val aliases: List<String> = emptyList()
)
