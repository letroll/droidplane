package fr.julien.quievreux.droidplane2.model

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Square
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.ArrowUp
import compose.icons.fontawesomeicons.solid.Bookmark
import compose.icons.fontawesomeicons.solid.Edit
import compose.icons.fontawesomeicons.solid.HatWizard
import compose.icons.fontawesomeicons.solid.Image
import compose.icons.fontawesomeicons.solid.Link
import compose.icons.fontawesomeicons.solid.List
import compose.icons.fontawesomeicons.solid.MailBulk
import compose.icons.fontawesomeicons.solid.Paperclip
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Redo
import compose.icons.fontawesomeicons.solid.Video
import compose.icons.fontawesomeicons.solid.VolumeUp
import compose.icons.tablericons.Circle0
import compose.icons.tablericons.Circle1
import compose.icons.tablericons.Circle2
import compose.icons.tablericons.Circle3
import compose.icons.tablericons.Circle4
import compose.icons.tablericons.Circle5
import compose.icons.tablericons.Circle6
import compose.icons.tablericons.Circle7
import compose.icons.tablericons.Circle8
import compose.icons.tablericons.Circle9
import fr.julien.quievreux.droidplane2.res.AppRes as R

enum class NodeIcons(
    val text: String,
    val resId: Int,
    val imageVector: ImageVector,
) {
    Addition("addition", R.drawable.ic_launcher, FontAwesomeIcons.Solid.Plus),
    Attach("attach", R.drawable.ic_launcher, FontAwesomeIcons.Solid.Paperclip),
    Audio("audio", R.drawable.ic_launcher, FontAwesomeIcons.Solid.VolumeUp),
    Back("back", R.drawable.ic_launcher, FontAwesomeIcons.Solid.ArrowLeft),
    Bookmark("bookmark", R.drawable.ic_launcher, FontAwesomeIcons.Solid.Bookmark),
    Full0("full-0", R.drawable.ic_launcher, TablerIcons.Circle0),
    Full1("full-1", R.drawable.ic_launcher, TablerIcons.Circle1),
    Full2("full-2", R.drawable.ic_launcher, TablerIcons.Circle2),
    Full3("full-3", R.drawable.ic_launcher, TablerIcons.Circle3),
    Full4("full-4", R.drawable.ic_launcher, TablerIcons.Circle4),
    Full5("full-5", R.drawable.ic_launcher, TablerIcons.Circle5),
    Full6("full-6", R.drawable.ic_launcher, TablerIcons.Circle6),
    Full7("full-7", R.drawable.ic_launcher, TablerIcons.Circle7),
    Full8("full-8", R.drawable.ic_launcher, TablerIcons.Circle8),
    Full9("full-9", R.drawable.ic_launcher, TablerIcons.Circle9),
    Image("image", R.drawable.ic_launcher, FontAwesomeIcons.Solid.Image),
    Link("link", R.drawable.ic_launcher, FontAwesomeIcons.Solid.Link),
    List("list", R.drawable.ic_launcher, FontAwesomeIcons.Solid.List),
    Mail("mail", R.drawable.ic_launcher, FontAwesomeIcons.Solid.MailBulk),
    RicheText("riche-text", R.drawable.ic_launcher, FontAwesomeIcons.Solid.Edit),
    Unchecked("unchecked", R.drawable.ic_launcher, FontAwesomeIcons.Regular.Square),
    Up("up", R.drawable.ic_launcher, FontAwesomeIcons.Solid.ArrowUp),
    Redo("redo", R.drawable.ic_launcher, FontAwesomeIcons.Solid.Redo),
    Video("video", R.drawable.ic_launcher, FontAwesomeIcons.Solid.Video),
    Wizard("wizard", R.drawable.ic_launcher, FontAwesomeIcons.Solid.HatWizard),
}

@Deprecated(
    message = "Use NodeIconRegistry.resolve(iconName) instead",
    replaceWith = ReplaceWith("NodeIconRegistry.resolve(iconName)", "fr.julien.quievreux.droidplane2.model.NodeIconRegistry")
)
fun getNodeIconsResIdFromName(iconName: String): Int? = NodeIcons.entries.firstOrNull {
    it.text == iconName
}?.resId

@Deprecated(
    message = "Use NodeIconRegistry.resolve(iconName) instead",
    replaceWith = ReplaceWith("NodeIconRegistry.resolve(iconName)", "fr.julien.quievreux.droidplane2.model.NodeIconRegistry")
)
fun getNodeFontIconsFromName(iconName: String): ImageVector? = NodeIcons.entries.firstOrNull {
    it.text == iconName
}?.imageVector

@Preview
@Composable
fun NodeIconsPreview() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(NodeIcons.entries) { nodeIcon ->
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    Icon(
                        imageVector = nodeIcon.imageVector,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(24.dp)
                    )
                    Text("Item $nodeIcon")
                }
            }
        }
    }
}
