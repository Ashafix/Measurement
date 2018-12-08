package net.mpeters.measurement

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.widget.TextView
import android.view.View
import android.widget.RadioButton
import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import kotlin.random.Random
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONException

import java.util.*
import android.content.res.AssetFileDescriptor
import android.widget.Toast


class MainActivity : AppCompatActivity() {

    private val server = WebServer()
    private lateinit var requestQueue: RequestQueue
    private lateinit var textView: TextView
    private lateinit var textServerAddress: TextView
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var bluetoothDevice: BluetoothDevice
    private var mContext: Context? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        setContentView(R.layout.activity_main)
        requestQueue = Volley.newRequestQueue(this.applicationContext)
        textView = findViewById(R.id.textView)
        textServerAddress = findViewById(R.id.serverAddress)
        val myWebView: WebView = findViewById(R.id.webView)


        val webSettings = myWebView.getSettings()
        webSettings.javaScriptEnabled = true

        myWebView.loadUrl("file:///android_asset/www/index.html")
    }

    fun onRadioButtonClicked(view: View) {
        if (view is RadioButton) {
            val formTextView: TextView = findViewById(R.id.serverAddress)
            when (view.getId()) {
                R.id.radio_bluetooth -> {
                    formTextView.visibility = View.INVISIBLE
                    server.source = "bluetooth"
                }

                R.id.radio_http -> {
                    formTextView.visibility = View.VISIBLE
                    server.source = "http"

                }

                R.id.radio_random -> {
                    formTextView.visibility = View.INVISIBLE
                    server.source = "random"
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    private var rootObject = JSONObject()
    private fun getValues(method: String): JSONObject {

        if (method == "random") {
            rootObject = JSONObject()
            rootObject.put("a", (0 until 10).random())
            rootObject.put("b", (0 until 10).random())
            rootObject.put("c", (0 until 10).random())
        } else if (method == "http") {
            val myReq = JsonObjectRequest(
                Request.Method.GET,
                textServerAddress.text.toString(),
                null,
                createMyReqSuccessListener(),
                createMyReqErrorListener()
            )
            requestQueue.add(myReq)
        } else if (method == "bluetooth") {
            connect_to_bluetooth()
            var bluetoothMessage: ByteArray = ByteArray(256)
            bluetoothSocket.inputStream.read(bluetoothMessage, 0, 256)
            rootObject = JSONObject(bluetoothMessage.toString())
        }

        return rootObject

    }

    private fun connect_to_bluetooth() {
        var force_reset: Boolean = false
        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            force_reset = true

        }
        if (bluetoothDevice == null || force_reset) {
            bluetoothDevice = bluetoothAdapter!!.getRemoteDevice("98:D3:51:FD:96:8E")
            force_reset = true

        }
        if (bluetoothSocket == null || force_reset) {
            bluetoothSocket =
                    bluetoothDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-80000-00805f9b34fb"))
            bluetoothSocket.connect()
        }

    }

    private fun createMyReqSuccessListener(): Response.Listener<JSONObject> {
        return Response.Listener { response ->
            try {
                textView.text = response.toString()
                rootObject = response
            } catch (e: JSONException) {
                textView.text = "Parse error"
            }
        }
    }


    private fun createMyReqErrorListener(): Response.ErrorListener {
        return Response.ErrorListener { error -> textView.text = error.message }
    }

    private inner class WebServer : NanoHTTPD("localhost", 8080) {

        var source: String = "random"
        override fun serve(session: IHTTPSession): NanoHTTPD.Response {
            var uri = session.uri.toString()
            //val toast = Toast.makeText(applicationContext, uri, Toast.LENGTH_LONG)
            //toast.show()
            textView.text = uri

            if (uri == "" || uri == "/") {
                uri = "/www/index.html"
            }
            if ((uri.endsWith(".html") || uri.endsWith(".js") || uri.endsWith(".css") || uri.endsWith(".ico")) && uri.startsWith("/www/")) {
                val fd = assets.openFd(uri.substring(1))

                var mbuffer = mContext!!.getAssets().open(uri.substring(1))
                var mime: String
                when (uri.takeLast(3)) {
                    "tml" -> mime = "text/html"
                    "css" -> mime = "text/css"
                    ".js" -> mime = "application/javascript"
                    "ico" -> mime = "image/x-icon"
                    else -> {
                        mime = ""
                    }
                }
                textView.text = "mime: " + uri + mime
                return newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mime, mbuffer, fd.getLength())


            }
            else if (uri.startsWith("/api/button/")) {
                val toast = Toast.makeText(applicationContext, "button", Toast.LENGTH_LONG)

            }
            else {
                getValues(source)
                val response = newFixedLengthResponse(rootObject.toString())
                response.addHeader("Access-Control-Allow-Origin", "*")
                return response
            }

            return newFixedLengthResponse("meh")

        }



    }
}

