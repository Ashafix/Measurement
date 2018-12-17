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
    private lateinit var textView: TextView
    private lateinit var buttonServer: Button
    private lateinit var buttonTerminator: Button
    private lateinit var measurementUpdater: MeasurementsUpdater
    private lateinit var dataProvider: DataProvider
    private lateinit var tableRowBluetooth: TableRow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        requestQueue = Volley.newRequestQueue(this)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.heading)
        buttonServer = findViewById(R.id.buttonServer)
        buttonTerminator = findViewById(R.id.buttonTerminator)
        tableRowBluetooth = findViewById(R.id.tableRowBluetooth)

        dataProvider = DataProvider(requestQueue,
            "random",
            "json",
            findViewById<TextView>(R.id.editTextServerAddress).text.toString(),
            getString(R.string.defaultDelimiter), getString(R.string.defaultTerminator))
        server = WebServer(mContext as Context, dataProvider)
        server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
        val webView: WebView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("file:///android_asset/www/index.html")
        measurementUpdater = MeasurementsUpdater(findViewById(android.R.id.content),this)
        measurementUpdater.run()
    }

    fun onButtonBluetoothSettingsClicked(view: View)
    {
        findViewById<RadioButton>(R.id.radioBluetooth).performClick()
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
                    buttonTerminator.visibility = View.VISIBLE
                    tableRowBluetooth.visibility = View.VISIBLE
                    dataProvider.dataSource = "bluetooth"

                    val sep = ": "
                    val bluetoothDevices: SortedMap<String, String>
                    try {
                        bluetoothDevices = dataProvider.getBluetoothDevices()
                    } catch (e: DataProviderException) {
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
                        dataProvider.bluetoothAddress = deviceAddress
                        try {
                            dataProvider.connectToBluetooth(forceReset = true)
                            findViewById<RadioButton>(R.id.radioBluetooth).text = findViewById<RadioButton>(R.id.radioBluetooth).text.split(sep)[0] + sep + deviceAddress
                        } catch (e: DataProviderException) {
                            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                        }
                        listview.visibility = View.GONE
                    }
                }

                R.id.radioHttp -> {
                    textServerAddress.visibility = View.VISIBLE
                    buttonServer.visibility = View.VISIBLE
                    textTerminator.visibility = View.INVISIBLE
                    buttonTerminator.visibility = View.INVISIBLE
                    dataProvider.dataSource = "http"
                    tableRowBluetooth.visibility = View.GONE
                }

                R.id.radioRandom -> {
                    textServerAddress.visibility = View.INVISIBLE
                    buttonServer.visibility = View.INVISIBLE
                    textTerminator.visibility = View.INVISIBLE
                    buttonTerminator.visibility = View.INVISIBLE
                    dataProvider.dataSource = "random"
                    tableRowBluetooth.visibility = View.GONE
                }
            }
        }
    }

    fun onRadioButtonDataFormatClicked(view: View) {
        if (view is RadioButton) {
            var visible = View.GONE
            when (view.getId()) {
                R.id.radioButtonFormatJson -> {
                    dataProvider.dataFormat = "json"
                }

                R.id.radioButtonFormatDelimited -> {
                    dataProvider.dataFormat = "csv"
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
        for (i in 1 until numberRows - 1)
        {
            table.getChildAt(i).visibility = if (showSettings) View.VISIBLE else View.GONE
        }

        //change app heading
        textView.text = if (showSettings) getString(R.string.textSelectSource) else getString(R.string.textClickSelectSource)
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
        dataProvider.serverAddress = findViewById<TextView>(R.id.editTextServerAddress).text.toString()
        hideKeyboard(view)
    }

    fun onButtonBluetoothClicked(view: View) {
        dataProvider.terminator = findViewById<TextView>(R.id.textTerminator).text.toString()
        hideKeyboard(view)
    }

    fun onButtonDelimiterClicked(view: View) {
        dataProvider.delimiter = findViewById<TextView>(R.id.textDelimiter).text.toString()
        hideKeyboard(view)
    }

    private fun hideKeyboard(view: View) {
        val imm = mContext!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
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
            dataProvider.getValues()
            val table = view.findViewById<TableLayout>(R.id.tableMeasurements)
            for (k in dataProvider.currentMeasurement.keys()) {
                if (k == "millis" || k == "timestamp") {
                    continue
                }

                val headerText = "$k: "

                if (table.findViewWithTag<TextView>(headerText) == null) {
                    val row = TableRow(context)
                    val colHeader = TextView(context)
                    val colValue = TextView(context)
                    colHeader.text = headerText
                    colHeader.tag = headerText
                    colValue.text = dataProvider.currentMeasurement.getString(k)
                    row.addView(colHeader)
                    row.addView(colValue)
                    table.addView(row)
                } else {
                    val row = table.findViewWithTag<TextView>(headerText).parent as TableRow
                    val textValue = row.getChildAt(1) as TextView
                    textValue.text = dataProvider.currentMeasurement.getString(k)
                }
            }
            run()
        }
    }


}

