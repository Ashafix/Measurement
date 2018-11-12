package net.mpeters.measurement

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.widget.TextView
import android.support.v4.os.HandlerCompat.postDelayed
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import kotlin.random.Random


class MainActivity : AppCompatActivity() {

    private val server = WebServer()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        setContentView(R.layout.activity_main)

        val textView : TextView = findViewById(R.id.textView)
        val myWebView: WebView = findViewById(R.id.webView)

        val webSettings = myWebView.getSettings()
        webSettings.javaScriptEnabled = true

        myWebView.loadUrl("file:///android_asset/www/index.html")
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    private inner class WebServer : NanoHTTPD(8080) {

        override fun serve(session: IHTTPSession): Response
        {
            val rootObject= JSONObject()
            rootObject.put("a",(0 until 10).random())
            rootObject.put("b",(0 until 10).random())
            rootObject.put("c", (0 until 10).random())
            val response =  newFixedLengthResponse(rootObject.toString())
            response.addHeader("Access-Control-Allow-Origin","*")
            return response


        }
        fun IntRange.random() = Random.nextInt(start, endInclusive + 1)
    }
}

