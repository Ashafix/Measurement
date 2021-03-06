package net.mpeters.measurement

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.*
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import fi.iki.elonen.NanoHTTPD
import android.view.View.OnFocusChangeListener
import android.widget.ArrayAdapter
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mContext: Context
    private lateinit var server: WebServer
    private lateinit var requestQueue: RequestQueue
    private lateinit var textHeading: TextView
    private lateinit var buttonServer: Button
    private lateinit var measurementUpdater: MeasurementsUpdater
    private lateinit var dataHandler: DataHandler
    private lateinit var tableRowBluetooth: TableRow

    private val sep = ": "

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        requestQueue = Volley.newRequestQueue(this)
        setContentView(R.layout.activity_main)

        findViews()

        dataHandler = DataHandler(requestQueue,
            "random",
            "json",
            findViewById<TextView>(R.id.editTextServerAddress).text.toString(),
            getString(R.string.defaultDelimiter), getString(R.string.defaultTerminator))
        server = WebServer(mContext, dataHandler)
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)

        clearResults()
        measurementUpdater = MeasurementsUpdater(findViewById(android.R.id.content),this)
        measurementUpdater.run()
    }

    fun findViews()
    {
        textHeading = findViewById(R.id.heading)
        buttonServer = findViewById(R.id.buttonServer)
        tableRowBluetooth = findViewById(R.id.tableRowBluetooth)
    }
    fun onRadioButtonDataSourceClicked(view: View) {
        if (view is RadioButton) {
            val textServerAddress: TextView = findViewById(R.id.editTextServerAddress)
            textServerAddress.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
            val textTerminator: TextView = findViewById(R.id.textTerminator)
            textTerminator.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    hideKeyboard(view)
                }
            }
            when (view.getId()) {

                R.id.radioBluetooth -> {
                    textServerAddress.visibility = View.INVISIBLE
                    buttonServer.visibility = View.INVISIBLE
                    textTerminator.visibility = View.VISIBLE

                    tableRowBluetooth.visibility = View.VISIBLE
                    dataHandler.dataSource = "bluetooth"

                    val bluetoothDevices: SortedMap<String, String>
                    try {
                        bluetoothDevices = dataHandler.getBluetoothDevices()
                    } catch (e: DataHandlerException) {
                        Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                        return
                    }
                    var devices = ArrayList<String>()
                    for (device in bluetoothDevices)
                    {
                        var name = device.key + sep + device.value
                        devices.add(name)

                    }
                    var adapter = ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_list_item_1,
                        devices
                    )

                    val listview = findViewById<ListView>(R.id.listViewBluetoothDevices)
                    listview.visibility = View.VISIBLE
                    listview.adapter = adapter
                    listview.setOnItemClickListener { _, _, position, _ ->

                        var cells = devices[position].split(sep)
                        var deviceName =  cells.take(cells.size - 1).joinToString(separator = sep)
                        var deviceAddress =  cells.last()
                        Toast.makeText(applicationContext, "Connecting to: $deviceName", Toast.LENGTH_SHORT).show()
                        dataHandler.bluetoothAddress = deviceAddress
                        try {
                            dataHandler.connectToBluetooth(forceReset = true)
                            findViewById<RadioButton>(R.id.radioBluetooth).text = findViewById<RadioButton>(R.id.radioBluetooth).text.split(sep)[0] + sep + deviceName
                        } catch (e: DataHandlerException) {
                            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                        }
                        listview.visibility = View.GONE
                    }
                    clearResults()
                }

                R.id.radioHttp -> {
                    textServerAddress.visibility = View.VISIBLE
                    buttonServer.visibility = View.VISIBLE
                    textTerminator.visibility = View.INVISIBLE
                    dataHandler.dataSource = "http"
                    tableRowBluetooth.visibility = View.GONE
                    clearResults()
                }

                R.id.radioRandom -> {
                    textServerAddress.visibility = View.INVISIBLE
                    buttonServer.visibility = View.INVISIBLE
                    textTerminator.visibility = View.INVISIBLE
                    dataHandler.dataSource = "random"
                    tableRowBluetooth.visibility = View.GONE
                    clearResults()
                }
            }
        }
    }

    fun onRadioButtonDataFormatClicked(view: View) {
        if (view is RadioButton) {
            var visible = View.GONE
            when (view.getId()) {
                R.id.radioButtonFormatJson -> {
                    dataHandler.dataFormat = "json"
                }

                R.id.radioButtonFormatDelimited -> {
                    dataHandler.dataFormat = "csv"
                    visible = View.VISIBLE
                }
            }
            findViewById<TextView>(R.id.textDelimiter).visibility = visible
            findViewById<Button>(R.id.buttonDelimiter).visibility = visible
        }
    }

    fun onRadioButtonSettingsClicked(view: View) {
        var table = findViewById<TableLayout>(R.id.MainTable)
        var numberRows = table.childCount
        val showSettings = table.getChildAt(1).visibility == View.GONE

        // hide all rows except for the first one (settings) and last one (results)
        for (i in 1 until numberRows - 1)
        {
            table.getChildAt(i).visibility = if (showSettings) View.VISIBLE else View.GONE
        }

        //change app heading
        textHeading.text = if (showSettings) getString(R.string.textSelectSource) else getString(R.string.textClickSelectSource)
    }

    fun onRadioButtonViewClicked(view: View) {
        if (view is RadioButton) {
            when (view.getId()) {
                R.id.radioGraph -> {
                    findViewById<WebView>(R.id.webView).visibility = View.VISIBLE
                    findViewById<TableLayout>(R.id.tableMeasurements).visibility = View.GONE
                }
                R.id.radioText -> {
                    findViewById<WebView>(R.id.webView).visibility = View.GONE
                    findViewById<TableLayout>(R.id.tableMeasurements).visibility = View.VISIBLE
                }
            }
        }
    }

    fun onButtonServerClicked(view: View) {
        dataHandler.serverAddress = findViewById<TextView>(R.id.editTextServerAddress).text.toString()
        hideKeyboard(view)
    }

    fun onButtonBluetoothClicked(view: View) {
        dataHandler.terminator = findViewById<TextView>(R.id.textTerminator).text.toString()
        hideKeyboard(view)
    }

    fun onButtonDelimiterClicked(view: View) {
        dataHandler.delimiter = findViewById<TextView>(R.id.textDelimiter).text.toString()
        hideKeyboard(view)
    }

    private fun hideKeyboard(view: View) {
        val imm = mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun clearResults() {
        var webView: WebView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("about:blank")
        webView.loadUrl("file:///android_asset/www/index.html")
        val table = findViewById<TableLayout>(R.id.tableMeasurements)
        table.removeAllViews()
    }

    override fun onDestroy() {
        super.onDestroy()
        server.stop()
    }

    inner class MeasurementsUpdater(view: View, val context: Context): Runnable {

        private val handler = Handler()
        var active = true // not needed at the moment but could be handy later

        override fun run() {
            if (active) {
                handler.postDelayed(updateResultTable, 1000)
            }
        }

        private var updateResultTable = Runnable()
        {
            dataHandler.getValues()
            val table = view.findViewById<TableLayout>(R.id.tableMeasurements)

            var keys = dataHandler.currentMeasurement.keys().asSequence().toMutableList()
            keys.sort()
            val indexTimestamp = keys.indexOf("timestamp")
            if (indexTimestamp > -1)
            {

                var timestamp = keys[indexTimestamp]
                keys.remove("timestamp")
                keys.add(0, timestamp)
            }

            for (k in keys) {
                if (k == "millis") {
                    continue
                }

                val headerText = "$k$sep"

                if (table.findViewWithTag<TextView>(headerText) == null) {
                    val row = TableRow(context)
                    val colHeader = TextView(context)
                    val colValue = TextView(context)
                    colHeader.text = headerText
                    colHeader.tag = headerText
                    colValue.text = dataHandler.currentMeasurement.getString(k)
                    row.addView(colHeader)
                    row.addView(colValue)
                    table.addView(row)
                } else {
                    val row = table.findViewWithTag<TextView>(headerText).parent as TableRow
                    val textValue = row.getChildAt(1) as TextView
                    textValue.text = dataHandler.currentMeasurement.getString(k)
                }
            }

            // remove table rows which are not in current measurement
            var allRowsOk = false
            while (!allRowsOk)
            {
                for (i in 0 until table.childCount) {
                    val row = table.getChildAt(i) as TableRow
                    val colHeader = row.getChildAt(0) as TextView
                    val measurementName = colHeader.text.take(colHeader.text.length - 2) as String
                    if (!dataHandler.currentMeasurement.has(measurementName))
                    {
                        table.removeView(row)
                        break
                    }
                }
                allRowsOk = true
            }

            run()
        }
    }


}

