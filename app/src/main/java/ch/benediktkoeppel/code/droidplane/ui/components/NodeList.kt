package ch.benediktkoeppel.code.droidplane.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.benediktkoeppel.code.droidplane.model.MindmapNode

fun LazyListScope.nodeList(
    nodes: List<MindmapNode>,
    fetchText: (MindmapNode) -> String?,
    onNodeClick: (MindmapNode) -> Unit,
) {
    items(nodes) { node ->
        fetchText(node)?.let { text ->
            Card(
                modifier = Modifier.padding(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                )
            ) {
                var expanded by rememberSaveable { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .clickable {
                            onNodeClick(node)
                        },
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    //TODO handle icon
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 4.dp,
                                vertical = 10.dp
                            ),
                        text = text
                    )
                    ToggleIcon()
                }
            }
        }
    }
}

@Composable
@Preview
private fun NodeListPreview(){
    //TODO TODO
}