package com.kegel.booker.book;

import android.content.Context;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.kegel.booker.utils.OnDownloadDone;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookUpdater {
    private final Context context;
    private final List<BookInfo> bookList;
    private OnDownloadDone completeHandler;

    public BookUpdater(Context context) {
        this.context = context;
        this.bookList = Helpers.getBooks(context);
        this.completeHandler = null;
    }

    public BookUpdater(Context context, OnDownloadDone completeHandler) {
        this(context);
        this.completeHandler = completeHandler;

    }


    public void process() throws MalformedURLException, URISyntaxException {
        String url = Helpers.getURL(context, "progress");
        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest jsonRequest = new StringRequest(Request.Method.POST, url, (Response.Listener<String>) response -> {
            Gson gson = new Gson();
            HashMap<String, Object> res = gson.fromJson(response, HashMap.class);
            List<BookInfo> newBookList = new ArrayList<>();
            for (BookInfo book : bookList) {
                if (res.containsKey(book.getCrc())) {
                    JsonObject bookObject = gson.toJsonTree(res.get(book.getCrc())).getAsJsonObject();
                    if (!bookObject.has("update") || !bookObject.get("update").getAsBoolean()) {
                        continue;
                    }
                    newBookList.add(new BookInfo.Builder(book)
                            .setChapter(bookObject.get("chapter").getAsInt())
                            .setPosition(bookObject.get("position").getAsLong())
                            .build()
                    );
                }
                else {
                    newBookList.add(book);
                }
            }
            Helpers.setBooks(context, newBookList);
            if (completeHandler != null) {
                completeHandler.complete("SUCCESS");
            }
        }, error -> {
            if (completeHandler != null) {
                completeHandler.complete("FAIL");
            }
        }
        ) {

            @Nullable
            @Override
            protected Map<String, String> getParams()  {
                Map<String, String> map = new HashMap<>();
                for (BookInfo book : bookList) {
                    map.put("DEVICE", "PHONE");
                    map.put(book.getCrc(), book.toString());
                }
                return map;
            }
        };


        queue.add(jsonRequest);
    }

}
