package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fr.julien.quievreux.droidplane2.res.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julien.quievreux.droidplane2.model.NodeIconRegistry
import fr.julien.quievreux.droidplane2.model.NodeIconVisual
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

/**
 * Renders a single Freeplane node icon visual representation:
 * - [NodeIconVisual.Vector]: Rendered via Icon with ImageVector
 * - [NodeIconVisual.Resource]: Rendered via Icon with multi-color painterResource
 * - [NodeIconVisual.Unmapped]: Rendered as a compact textual badge [name]
 */
@Composable
fun NodeIconView(
    visual: NodeIconVisual,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 16.dp
) {
    when (visual) {
        is NodeIconVisual.Vector -> {
            Icon(
                imageVector = visual.imageVector,
                contentDescription = contentDescription,
                modifier = modifier.size(size),
                tint = tint ?: LocalContentColor.current
            )
        }
        is NodeIconVisual.Resource -> {
            Icon(
                painter = painterResource(visual.resId),
                contentDescription = contentDescription,
                modifier = modifier.size(size),
                tint = tint ?: Color.Unspecified
            )
        }
        is NodeIconVisual.Unmapped -> {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = modifier
            ) {
                Text(
                    text = "[${visual.name}]",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Convenience overload that automatically resolves an icon string via [NodeIconRegistry].
 */
@Composable
fun NodeIconView(
    iconName: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = iconName,
    tint: Color? = null,
    size: Dp = 16.dp
) {
    NodeIconView(
        visual = NodeIconRegistry.resolve(iconName),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint,
        size = size
    )
}

@Preview
@Composable
fun PreviewNodeIconView() {
    ContrastAwareReplyTheme {
        Surface {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Vector icon
                NodeIconView(iconName = "full-1", size = 20.dp)
                // Resource drawable icon
                NodeIconView(iconName = "flag-red", size = 20.dp)
                NodeIconView(iconName = "button_ok", size = 20.dp)
                // Unmapped fallback badge
                NodeIconView(iconName = "unknown_icon", size = 20.dp)
            }
        }
    }
}
