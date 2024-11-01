package fr.julien.quievreux.droidplane2.model

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Square
import compose.icons.fontawesomeicons.solid.ArrowLeft
import compose.icons.fontawesomeicons.solid.ArrowUp
import compose.icons.fontawesomeicons.solid.AudioDescription
import compose.icons.fontawesomeicons.solid.Backward
import compose.icons.fontawesomeicons.solid.Bookmark
import compose.icons.fontawesomeicons.solid.Check
import compose.icons.fontawesomeicons.solid.Edit
import compose.icons.fontawesomeicons.solid.FileAudio
import compose.icons.fontawesomeicons.solid.FileVideo
import compose.icons.fontawesomeicons.solid.HatWizard
import compose.icons.fontawesomeicons.solid.Image
import compose.icons.fontawesomeicons.solid.Link
import compose.icons.fontawesomeicons.solid.List
import compose.icons.fontawesomeicons.solid.MailBulk
import compose.icons.fontawesomeicons.solid.Paperclip
import compose.icons.fontawesomeicons.solid.PhoneVolume
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Redo
import compose.icons.fontawesomeicons.solid.Square
import compose.icons.fontawesomeicons.solid.Video
import compose.icons.fontawesomeicons.solid.VideoSlash
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
import fr.julien.quievreux.droidplane2.R
import kotlin.io.path.name
import kotlin.reflect.KProperty1

enum class NodeIcons(
    val text: String,
    val resId: Int,
    val imageVector: ImageVector,
) {
    //TODO bind all icon
    //TODO remove all drawable
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

    //    Help("help", R.drawable.ic_launcher, FontAwesomeIcons.Regular),
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

fun getNodeIconsResIdFromName(iconName: String): Int? = NodeIcons.entries.firstOrNull {
    it.text == iconName
}?.resId

fun getNodeFontIconsFromName(iconName: String): ImageVector? = NodeIcons.entries.firstOrNull {
    it.text == iconName
}?.imageVector

fun getDrawableFileName(context: Context, resourceId: Int): String? {
    return context.resources.getResourceEntryName(resourceId)
}

fun getDrawableFileNames(context: Context): List<Pair<String, Int>> {
    val fileNames = mutableListOf<Pair<String, Int>>()
    val resources = context.resources
    val drawableId = resources.getIdentifier("drawable", "drawable", context.packageName)
    val drawableFields = R.drawable::class.java.fields
    for (field in drawableFields) {
        try {
            val resourceId = field.getInt(null)
            if (resourceId != 0 && resourceId >= drawableId) {
                fileNames.add(Pair(resources.getResourceEntryName(resourceId), resourceId))
            }
        } catch (e: Exception) {
            // Ignorer les erreurs
        }
    }
    return fileNames
}

fun getDrawableFileNames(drawableClass: Class<*>): List<OldIcon> {
    val intProperties = mutableListOf<OldIcon>()
    for (field in drawableClass.declaredFields) {
        try {
            field.isAccessible = true
            val propertyName = field.name
            val propertyValue = field.getInt(null)
            intProperties.add(OldIcon(propertyName, propertyValue))
        } catch (e: IllegalAccessException) {
            println("Error accessing field: ${field.name}")
        }
    }
    return intProperties
}

data class OldIcon(val name: String, val resId: Int)

@Preview
@Composable
fun NodeIconsPreview() {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), //  colonnes
        contentPadding = PaddingValues(16.dp), // Padding autour de la grille
        verticalArrangement = Arrangement.spacedBy(8.dp), // Espacement vertical entre les éléments
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Espacement horizontal entre les éléments
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

@Preview
@Composable
fun NodeToIconsPreview() {
    val drawableProperties = getDrawableFileNames(R.drawable::class.java)
    Text("Item ${drawableProperties.size} ")
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), //  colonnes
        contentPadding = PaddingValues(16.dp), // Padding autour de la grille
        verticalArrangement = Arrangement.spacedBy(8.dp), // Espacement vertical entre les éléments
        horizontalArrangement = Arrangement.spacedBy(8.dp) // Espacement horizontal entre les éléments
    ) {
        items(drawableProperties) { oldIcon ->
            Card {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                ) {
                    if (oldIcon.name.startsWith("icon")) {
                        if (oldIcon.resId > 0) {
                            Icon(
                                painter = painterResource(oldIcon.resId),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(12.dp)
                            )
                        }
                        Text("Item ${oldIcon.name}")
                    }
                }
            }
        }
    }
}
