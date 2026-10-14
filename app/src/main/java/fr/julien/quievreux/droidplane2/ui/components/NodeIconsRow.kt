package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

/**
 * Renders an ordered row of Freeplane node icons.
 * If [iconNames] has more items than [maxInlineIcons], displays the first [maxInlineIcons]
 * followed by a compact "+N" overflow badge.
 */
@Composable
fun NodeIconsRow(
    iconNames: List<String>,
    modifier: Modifier = Modifier,
    maxInlineIcons: Int = Int.MAX_VALUE,
    iconSize: Dp = 16.dp,
    spacing: Dp = 4.dp
) {
    if (iconNames.isEmpty()) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayed = if (iconNames.size > maxInlineIcons) {
            iconNames.take(maxInlineIcons)
        } else {
            iconNames
        }

        displayed.forEach { iconName ->
            NodeIconView(
                iconName = iconName,
                size = iconSize
            )
        }

        if (iconNames.size > maxInlineIcons) {
            val overflowCount = iconNames.size - maxInlineIcons
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Text(
                    text = "+$overflowCount",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

@Preview(name = "NodeIconsRow Previews", showBackground = true)
@Composable
fun PreviewNodeIconsRow() {
    ContrastAwareReplyTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Single Icon:")
                NodeIconsRow(
                    iconNames = listOf("button_ok"),
                    maxInlineIcons = 3
                )

                Text("3 Icons (exact fit in list):")
                NodeIconsRow(
                    iconNames = listOf("button_ok", "flag-red", "full-1"),
                    maxInlineIcons = 3
                )

                Text("5 Icons with Overflow in NodeList (max 3):")
                NodeIconsRow(
                    iconNames = listOf("button_ok", "flag-red", "full-1", "idea", "attach"),
                    maxInlineIcons = 3
                )

                Text("5 Icons in MindMap Canvas (all visible):")
                NodeIconsRow(
                    iconNames = listOf("button_ok", "flag-red", "full-1", "idea", "attach"),
                    maxInlineIcons = Int.MAX_VALUE
                )

                Text("With Unmapped Icon:")
                NodeIconsRow(
                    iconNames = listOf("flag-green", "custom_unknown_tag"),
                    maxInlineIcons = 3
                )
            }
        }
    }
}
