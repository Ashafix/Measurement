package net.mpeters.measurement

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class DataProvider(private var requestQueue: RequestQueue,
                   var dataSource: String,
                   var dataFormat: String,
                   var serverAddress: String,
                   var delimiter: String,
                   var terminator: String) {

    var currentMeasurement = JSONObject()
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothSocket: BluetoothSocket
    private lateinit var bluetoothDevice: BluetoothDevice
    private val bluetoothStringBuilder = StringBuilder()
    var bluetoothAddress = ""

    fun getValues(): JSONObject {

        when (dataSource) {
            "random" -> {
                val calendar = Calendar.getInstance().time
                val format = SimpleDateFormat("h:mm:ss")
                currentMeasurement = JSONObject()
                currentMeasurement.put("Temperature", (10 until 40).random())
                currentMeasurement.put("Humidity", (0 until 100).random())
                currentMeasurement.put("Salinity", (100 until 600).random())
                currentMeasurement.put("pH", (4 until 8).random())
                currentMeasurement.put("timestamp", format.format(calendar.time))
                currentMeasurement.put("millis", format.format(calendar.time))
            }
            "http" -> {
                if (dataFormat == "json") {
                    val myReq = JsonObjectRequest(
                        Request.Method.GET,
                        "$serverAddress/api/measurement",
                        null,
                        createMyReqSuccessListenerJson(),
                        createMyReqErrorListener())
                    requestQueue.add(myReq)
                } else {
                    val myReq = StringRequest(
                        Request.Method.GET,
                        "$serverAddress/api/measurement",
                        createMyReqSuccessListenerCsv(),
                        createMyReqErrorListener())
                    requestQueue.add(myReq)
                }
            }
            "bluetooth" -> {
                try {
                    val bytesAvailable = bluetoothSocket.inputStream.available()
                    if (bytesAvailable == 0) {
                        return currentMeasurement
                    }

                    val bluetoothMessage = ByteArray(bytesAvailable)
                    bluetoothSocket.inputStream.read(bluetoothMessage, 0, bytesAvailable)
                    bluetoothStringBuilder.append(bluetoothMessage.toString(Charsets.US_ASCII))

                    if (bluetoothStringBuilder.indexOf(terminator) == bluetoothStringBuilder.lastIndexOf(terminator)) {
                        return currentMeasurement
                    }
                    var bluetoothString = bluetoothStringBuilder.toString()
                    bluetoothString = bluetoothString.substring(bluetoothString.indexOf(terminator) + 1)
                    bluetoothString = bluetoothString.substring(0, bluetoothString.indexOf(terminator) + 1)

                    bluetoothStringBuilder.clear()
                    try {
                        currentMeasurement = JSONObject(bluetoothString)
                    } catch (e: JSONException) {
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

    fun getBluetoothDevices(): SortedMap<String, String> {
        if (!::bluetoothAdapter.isInitialized) {
            try {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            } catch (e: Exception) {
                throw DataProviderException("Could not find Bluetooth device.")
            }
        }

        var bluetoothDevices = HashMap<String, String>()
        for (bt in bluetoothAdapter.bondedDevices)
        {
            bluetoothDevices[bt.name] = bt.address
        }
        return bluetoothDevices.toSortedMap()
    }

    fun connectToBluetooth(forceReset: Boolean = false) {

        var forceReset = forceReset
        if (!::bluetoothAdapter.isInitialized || forceReset) {
            try {
                bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                forceReset = true
            } catch (e: Exception) {
                throw DataProviderException("Could not find Bluetooth device.")
            }
        }
        if (!::bluetoothDevice.isInitialized || forceReset) {
            try {
                bluetoothDevice = bluetoothAdapter!!.getRemoteDevice(bluetoothAddress)
                forceReset = true
            } catch (e: Exception) {
                throw DataProviderException("Could not find remote Bluetooth device.")
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

            } catch (e: Exception) {
                throw DataProviderException(e.message.toString())
            }
            try {
                bluetoothSocket.connect()
            } catch (e: Exception) {
                throw DataProviderException("Failed to connect: $e.message")
            }
        }
    }

    private fun createMyReqSuccessListenerJson(): Response.Listener<JSONObject> {
        return Response.Listener { response ->
            try {
                currentMeasurement = response
            } catch (e: JSONException) {
                throw DataProviderException("Parse error")
            }
        }
    }

    private fun createMyReqSuccessListenerCsv(): Response.Listener<String> {
        return Response.Listener { response ->
            try {
                currentMeasurement = JSONObject()
                val values = response.split(delimiter)
                var index = 0
                for (v in values) {
                    index++
                    currentMeasurement.put("Value $index", v)
                }
            } catch (e: JSONException) {
                throw DataProviderException("Parse error")
            }
        }
    }

    private fun createMyReqErrorListener(): Response.ErrorListener {
        return Response.ErrorListener {  }
    }
}

class DataProviderException(message:String): Exception(message)