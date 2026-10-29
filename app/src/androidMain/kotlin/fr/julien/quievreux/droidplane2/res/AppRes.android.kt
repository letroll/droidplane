package fr.julien.quievreux.droidplane2.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource as androidPainterResource

@Composable
actual fun painterResource(id: Int): Painter {
    val context = LocalContext.current
    val name = AppRes.drawableNames[id]
    val androidResId = remember(id, name) {
        if (name != null) {
            context.resources.getIdentifier(name, "drawable", context.packageName)
        } else {
            id
        }
    }
    return if (androidResId != 0) {
        androidPainterResource(androidResId)
    } else {
        ColorPainter(Color.Transparent)
    }
}
