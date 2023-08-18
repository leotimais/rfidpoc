package com.buck.leonardo.rfidpoc.service;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class RequestService {
    private static RequestService instance;
    private RequestQueue requestQueue;
    private static Context ctx;

    private RequestService(Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized RequestService getInstance(Context context) {
        if (instance == null) {
            instance = new RequestService(context);
        }
        return instance;
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return requestQueue;
    }

    public<T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
