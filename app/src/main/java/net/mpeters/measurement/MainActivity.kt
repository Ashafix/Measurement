package net.mpeters.measurement

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.*
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import fi.iki.elonen.NanoHTTPD
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

val mimeTypes = hashMapOf(
    "html" to "text/html",
    "css" to "text/css",
    "js" to "application/javascript",
    "ico" to "image/x-icon"
)


class MainActivity : AppCompatActivity() {

    private val server = WebServer()
    private lateinit var requestQueue: RequestQueue
    private lateinit var textView: TextView
    private lateinit var buttonServer: Button
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var bluetoothDevice: BluetoothDevice
    private var mContext: Context? = null
    private lateinit var serverAddress: String
    private var currentMeasurement = JSONObject()
    private val bluetoothStringBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        setContentView(R.layout.activity_main)
        requestQueue = Volley.newRequestQueue(this.applicationContext)
        textView = findViewById(R.id.heading)

        buttonServer = findViewById(R.id.serverButton)
        serverAddress = findViewById<TextView>(R.id.serverAddress).text.toString()
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
                    connectToBluetooth(forceReset=true)
                    formTextView.visibility = View.INVISIBLE
                    buttonServer.visibility = View.INVISIBLE
                    server.source = "bluetooth"
                }

                R.id.radio_http -> {
                    formTextView.visibility = View.VISIBLE
                    buttonServer.visibility = View.VISIBLE
                    server.source = "http"
                }

                R.id.radio_random -> {
                    formTextView.visibility = View.INVISIBLE
                    buttonServer.visibility = View.INVISIBLE
                    server.source = "random"
                }
            }
        }
    }

    fun onButtonDataSourceClicked(view: View) {
        var row = findViewById<TableRow>(R.id.Settings)
        if (row.visibility == View.VISIBLE) {
            row.visibility = View.GONE
            textView.text = getString(R.string.textSelectSource)
        }
        else {
            row.visibility = View.VISIBLE
            textView.text = getString(R.string.textClickSelectSource)
        }
    }

    fun onButtonClicked(view: View) {
        serverAddress = findViewById<TextView>(R.id.serverAddress).text.toString()
        val imm = mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    private fun getValues(method: String): JSONObject {

        when (method) {
            "random" -> {
                val calendar = Calendar.getInstance().time
                val format = SimpleDateFormat("h:mm:ss")
                currentMeasurement = JSONObject()
                currentMeasurement.put("Temperature", (10 until 40).random())
                currentMeasurement.put("Humidity", (0 until 100).random())
                currentMeasurement.put("Salinity", (100 until 600).random())
                currentMeasurement.put("timestamp", format.format(calendar.time))
                currentMeasurement.put("millis", format.format(calendar.time))
            }
            "http" -> {
                val myReq = JsonObjectRequest(
                    Request.Method.GET,
                    serverAddress + "/api/measurement",
                    null,
                    createMyReqSuccessListener(),
                    createMyReqErrorListener()
                )
                requestQueue.add(myReq)
            }
            "bluetooth" -> {
                try
                {
                    val bytesAvailable = bluetoothSocket.inputStream.available()
                    if (bytesAvailable == 0)
                    {
                        return currentMeasurement
                    }

                    val bluetoothMessage: ByteArray = ByteArray(bytesAvailable)
                    bluetoothSocket.inputStream.read(bluetoothMessage, 0, bytesAvailable)
                    bluetoothStringBuilder.append(bluetoothMessage.toString(Charsets.US_ASCII))

                    if (bluetoothStringBuilder.indexOf("\n") == bluetoothStringBuilder.lastIndexOf("\n"))
                    {
                        return currentMeasurement
                    }
                    var bluetoothString = bluetoothStringBuilder.toString()
                    bluetoothString = bluetoothString.substring(bluetoothString.indexOf("\n") + 1)
                    bluetoothString = bluetoothString.substring(0, bluetoothString.indexOf("\n") + 1)

                    bluetoothStringBuilder.clear()
                    try {
                        currentMeasurement = JSONObject(bluetoothString)
                    }
                    catch (e: JSONException) {
                        return currentMeasurement
                    }
                }
                catch (e: java.lang.Exception) {
                    return currentMeasurement
                }


            }
        }
        return currentMeasurement
    }

    private fun connectToBluetooth(forceReset: Boolean = false) {

        var forceReset = forceReset
        if (!::bluetoothAdapter.isInitialized || forceReset) {

            try {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                forceReset = true

            } catch (e: Exception){
                val text = "Could not find Bluetooth device."
                val duration = Toast.LENGTH_LONG

                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
                return
            }

        }
        if (!::bluetoothDevice.isInitialized || forceReset) {
            try {
                bluetoothDevice = bluetoothAdapter!!.getRemoteDevice("98:D3:51:FD:96:8E")
                forceReset = true
            } catch (e: Exception) {
                val text = "Could not find remote Bluetooth device."
                val duration = Toast.LENGTH_LONG

                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
                return
            }
        }

        if (!::bluetoothSocket.isInitialized || forceReset) {
            try {
                bluetoothSocket.close()
            } catch (e: Exception) {
                // that's fine
            }
            try {
            bluetoothSocket =
                    bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"))

            } catch (e: Exception){
                val text = e.message
                val duration = Toast.LENGTH_SHORT

                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
            }
            try {
                bluetoothSocket.connect()
            } catch (e: Exception){
                val text = "Failed to connect: " + e.message
                val duration = Toast.LENGTH_SHORT

                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
            }

        }

    }

    private fun createMyReqSuccessListener(): Response.Listener<JSONObject> {
        return Response.Listener { response ->
            try {
                currentMeasurement = response
            } catch (e: JSONException) {
                textView.text = "Parse error"
            }
        }
    }


    private fun createMyReqErrorListener(): Response.ErrorListener {
        return Response.ErrorListener { error ->
            textView.text = error.message
        }
    }

    private inner class WebServer : NanoHTTPD(null, 8080) {

        var source: String = "random"
        override fun serve(session: IHTTPSession): NanoHTTPD.Response {
            var uri = session.uri.toString()
            lateinit var response: NanoHTTPD.Response
            if (uri == "" || uri == "/") {
                uri = "/www/index.html"
            }
            val suffix = uri.substring(uri.lastIndexOf(".") + 1)

            if (uri.startsWith("/www/") && mimeTypes.keys.contains(suffix)) {
                val mbuffer = mContext!!.assets.open(uri.substring(1))
                response =  newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mimeTypes[suffix], mbuffer, mbuffer.available().toLong())

            } else if (uri.startsWith("/api/button/")) {
                //TODO
            } else if (uri.startsWith("/api/measurement")) {
                getValues(source)
                response = newFixedLengthResponse(currentMeasurement!!.toString())
                response.mimeType = "application/json"

            }
            else {
                response = newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, null, null)
            }

            response.addHeader("Access-Control-Allow-Origin", "*")
            return response
        }

    }


}

