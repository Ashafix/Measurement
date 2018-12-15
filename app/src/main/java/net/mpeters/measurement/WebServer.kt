package net.mpeters.measurement

import android.content.Context
import com.android.volley.VolleyError
import fi.iki.elonen.NanoHTTPD

val mimeTypes = hashMapOf(
    "html" to "text/html",
    "css" to "text/css",
    "js" to "application/javascript",
    "ico" to "image/x-icon"
)

class WebServer (context: Context, dataProvider: DataProvider, port: Int = 8080) : NanoHTTPD(null, port) {


    private val context = context
    private val dataProvider = dataProvider

    override fun serve(session: IHTTPSession): NanoHTTPD.Response {
        var uri = session.uri.toString()
        lateinit var response: NanoHTTPD.Response
        if (uri == "" || uri == "/") {
            uri = "/www/index.html"
        }
        val suffix = uri.substring(uri.lastIndexOf(".") + 1)

        if (uri.startsWith("/www/") && mimeTypes.keys.contains(suffix)) {
            val buffer = context!!.assets.open(uri.substring(1))
            response = newFixedLengthResponse(
                Response.Status.OK,
                mimeTypes[suffix],
                buffer,
                buffer.available().toLong()
            )

        } else if (uri.startsWith("/api/button/")) {
            //TODO
        } else if (uri.startsWith("/api/measurement")) {
            try {
                response = newFixedLengthResponse(dataProvider.getValues()!!.toString())
                response.mimeType = "application/json"
            } catch (e: VolleyError) {
                response = newFixedLengthResponse("something went wrong")
                response.mimeType = mimeTypes.get("html")

            }



        } else {
            response = newFixedLengthResponse(Response.Status.NOT_FOUND, null, null)
        }

        response.addHeader("Access-Control-Allow-Origin", "*")
        return response
    }
}


