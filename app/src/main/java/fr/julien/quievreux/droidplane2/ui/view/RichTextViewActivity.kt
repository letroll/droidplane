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
import fr.julien.quievreux.droidplane2.R
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
        setContentView(R.layout.activity_main)
        // get data from intent. Data has to be base64 encoded, otherwise WebView stops processing
        // at the first # character. See https://developer.android.com/reference/android/webkit/WebView#loadData(java.lang.String,%20java.lang.String,%20java.lang.String)
        // > For all other values of encoding (including null) it is assumed that the data uses
        // > ASCII encoding for octets inside the range of safe URL characters and use the standard
        // > %xx hex encoding of URLs for octets outside that range. See RFC 3986 for more
        // > information. Applications targeting Build.VERSION_CODES.Q or later must either use
        // > base64 or encode any # characters in the content as %23, otherwise they will be treated
        // > as the end of the content and the remaining text used as a document fragment
        // > identifier.
        val richTextContent = intent.getStringExtra("richTextContent")
        val encodedContent = Base64.encodeToString(richTextContent?.toByteArray(), Base64.NO_PADDING)

        // set data of web view
        setContent {
            WebView(
                modifier = Modifier.systemBarsPadding(),
                data = encodedContent,
                webViewContent = EncodedContent
            )
        }
    }
}

enum class WebViewContent{
    EncodedContent,Url
}


@Composable
fun WebView(
    data: String,
    webViewContent: WebViewContent,
    modifier: Modifier = Modifier,
){
    // Adding a WebView inside AndroidView
    // with layout as full screen
    AndroidView(
        modifier = modifier,
        factory = {
        WebView(it).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }, update = {
        when(webViewContent){
            EncodedContent -> it.loadData(data, "text/html", "base64")
            Url -> it.loadUrl(data)
        }
    })
}
