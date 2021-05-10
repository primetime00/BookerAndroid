package com.kegel.booker.utils;

import android.content.Context;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

public class InputStreamVolleyRequest extends Request<byte[]> {
    private final Response.Listener<byte[]> mListener;
    private Map<String, String> mParams;

    //create a static map for directly accessing headers
    public Map<String, String> responseHeaders ;

    public InputStreamVolleyRequest(int method, String mUrl, Context c, String name, OnDownloadDone doneListener, HashMap<String, String> params) {
        // TODO Auto-generated constructor stub

        super(method, mUrl, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                doneListener.complete(name);
            }
        });
        // this request would never use cache.
        setShouldCache(false);
        mListener = new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                try {
                    if (response!=null) {
                        FileOutputStream outputStream;
                        outputStream = c.openFileOutput(name, Context.MODE_PRIVATE);
                        outputStream.write(response);
                        outputStream.close();
                        doneListener.complete(name);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    Log.d("KEY_ERROR", "UNABLE TO DOWNLOAD FILE");
                    e.printStackTrace();
                    doneListener.complete(name);
                }
            }
        };
        mParams=params;
    }

    @Override
    protected Map<String, String> getParams()
            throws com.android.volley.AuthFailureError {
        return mParams;
    };


    @Override
    protected void deliverResponse(byte[] response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {

        //Initialise local responseHeaders map with response headers received
        responseHeaders = response.headers;

        //Pass the response data here
        return Response.success( response.data, HttpHeaderParser.parseCacheHeaders(response));
    }
}