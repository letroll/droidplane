package fr.julien.quievreux.droidplane2.view

import android.app.Activity
import android.os.Bundle
import android.util.Base64
import android.webkit.WebView
import fr.julien.quievreux.droidplane2.R

class RichTextViewActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rich_text_view)

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
        val webView = findViewById<WebView>(R.id.webview)

        webView.loadData(encodedContent, "text/html", "base64")
    }
}
