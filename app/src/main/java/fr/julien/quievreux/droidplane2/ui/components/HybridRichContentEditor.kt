package fr.julien.quievreux.droidplane2.ui.components

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

enum class EditorMode {
    PREVIEW,
    SOURCE,
}

@Composable
fun HybridRichContentEditor(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var editorMode by remember { mutableStateOf(EditorMode.PREVIEW) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Mode Header & Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = editorMode == EditorMode.PREVIEW,
                    onClick = { editorMode = EditorMode.PREVIEW },
                    label = { Text(stringResource(R.string.preview_mode), fontSize = 12.sp) }
                )

                FilterChip(
                    selected = editorMode == EditorMode.SOURCE,
                    onClick = { editorMode = EditorMode.SOURCE },
                    label = { Text(stringResource(R.string.source_mode), fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Formatting Toolbar (applicable in both modes)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onValueChange(wrapWithTag(value, "b")) },
                modifier = Modifier.size(36.dp)
            ) {
                Text("B", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            IconButton(
                onClick = { onValueChange(wrapWithTag(value, "i")) },
                modifier = Modifier.size(36.dp)
            ) {
                Text("I", fontStyle = FontStyle.Italic, fontSize = 16.sp)
            }
            IconButton(
                onClick = { onValueChange(wrapWithList(value)) },
                modifier = Modifier.size(36.dp)
            ) {
                Text("•", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.weight(1f))

            // Quick color indicators
            val sampleColors = listOf(
                "#000000" to Color.Black,
                "#D32F2F" to Color(0xFFD32F2F),
                "#1976D2" to Color(0xFF1976D2),
                "#388E3C" to Color(0xFF388E3C),
            )
            sampleColors.forEach { (hex, color) ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onValueChange("<font color=\"$hex\">$value</font>") }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Editor Area
        when (editorMode) {
            EditorMode.PREVIEW -> {
                val plainContent = remember(value) {
                    try {
                        Html.fromHtml(value).toString().trim()
                    } catch (e: Exception) {
                        value
                    }
                }
                OutlinedTextField(
                    value = plainContent,
                    onValueChange = { newPlainText ->
                        if (value.contains("<")) {
                            onValueChange("<html><head/><body><p>$newPlainText</p></body></html>")
                        } else {
                            onValueChange(newPlainText)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    label = { Text(label) },
                    placeholder = { Text("Enter content...") }
                )
            }

            EditorMode.SOURCE -> {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    label = { Text("$label (HTML Source)") },
                    placeholder = { Text("<html><body><p>...</p></body></html>") }
                )
            }
        }
    }
}

private fun wrapWithTag(text: String, tag: String): String {
    return if (text.isBlank()) {
        "<$tag></$tag>"
    } else {
        "<$tag>$text</$tag>"
    }
}

private fun wrapWithList(text: String): String {
    val items = text.split("\n").filter { it.isNotBlank() }
    return if (items.isEmpty()) {
        "<ul><li></li></ul>"
    } else {
        "<ul>" + items.joinToString("") { "<li>$it</li>" } + "</ul>"
    }
}

@Preview(name = "HybridRichContentEditor Preview")
@Composable
fun PreviewHybridRichContentEditor() {
    ContrastAwareReplyTheme {
        Surface {
            HybridRichContentEditor(
                value = "<html><body><p>Sample <b>rich content</b></p></body></html>",
                onValueChange = {},
                label = "Notes"
            )
        }
    }
}
