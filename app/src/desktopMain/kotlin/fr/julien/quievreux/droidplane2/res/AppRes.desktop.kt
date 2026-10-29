package fr.julien.quievreux.droidplane2.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap

@Composable
actual fun painterResource(id: Int): Painter {
    val name = AppRes.drawableNames[id] ?: return ColorPainter(Color.Transparent)
    val bitmap = remember(name) {
        val path = "drawable/$name.png"
        val stream = Thread.currentThread().contextClassLoader?.getResourceAsStream(path)
            ?: AppRes::class.java.classLoader?.getResourceAsStream(path)
        stream?.use { loadImageBitmap(it) }
    }
    return if (bitmap != null) {
        BitmapPainter(bitmap)
    } else {
        ColorPainter(Color.Transparent)
    }
}
