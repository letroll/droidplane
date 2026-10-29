package fr.julien.quievreux.droidplane2.ui.view

import android.os.Bundle
import android.util.Base64
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import fr.julien.quievreux.droidplane2.ui.theme.primaryContainerLight
import fr.julien.quievreux.droidplane2.ui.theme.primaryLight
import fr.julien.quievreux.droidplane2.ui.view.WebViewContent.EncodedContent
import fr.julien.quievreux.droidplane2.ui.view.WebViewContent.Url

class RichTextViewActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                scrim = primaryLight.toArgb(),
                darkScrim = primaryContainerLight.toArgb()
            )
        )
        val richTextContent = intent.getStringExtra("richTextContent")
        val encodedContent = Base64.encodeToString(richTextContent?.toByteArray(), Base64.NO_PADDING)

        setContent {
            WebView(
                modifier = Modifier.systemBarsPadding(),
                data = encodedContent,
                webViewContent = EncodedContent
            )
        }
    }
}

enum class WebViewContent {
    EncodedContent, Url
}

@Composable
fun WebView(
    data: String,
    webViewContent: WebViewContent,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = {
            when (webViewContent) {
                EncodedContent -> it.loadData(data, "text/html", "base64")
                Url -> it.loadUrl(data)
            }
        }
    )
}
