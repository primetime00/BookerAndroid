package com.kegel.booker.book;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.kegel.booker.utils.OnDownloadDone;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class BookRecorder implements Runnable, OnDownloadDone {

    private Handler timerHandler;

    private MediaPlayer mediaPlayer;
    private BookInfo book;

    private int prevPosition = -1;

    ReentrantLock lock = new ReentrantLock();

    private final Context context;


    public BookRecorder(Context context) {
        this.context = context;
        this.timerHandler = new Handler();
    }

    public void process(BookInfo book) throws MalformedURLException, URISyntaxException {
        this.book = book;
        String url = Helpers.getURL(context, "checkin");
        RequestQueue queue = Volley.newRequestQueue(context);

        StringRequest jsonRequest = new StringRequest(Request.Method.POST, url, response -> {
            Gson gson = new Gson();
            HashMap<String, Object> res = gson.fromJson(response, HashMap.class);
            complete("SUCCESS");
        }, error -> {
            complete("FAIL");
        }
        ) {

            @Nullable
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put(book.getCrc(), book.toString());
                return map;
            }
        };
        queue.add(jsonRequest);
    }

    public void start() {
        timerHandler.postDelayed(this, 500);

    }

    public void destroy() {
        timerHandler.removeCallbacks(this);
    }

    @Override
    public void run() {
        lock.lock();
        if (book == null || mediaPlayer == null) {
            timerHandler.postDelayed(this, 1000);
            return;
        }
        int pos = calculatePosition();
        if (Math.abs(pos - prevPosition) > 4) {
            prevPosition = pos;
            Log.d("timer", "Saving book progress");
            try {
                process(book);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            timerHandler.postDelayed(this, 1000);
        }
        lock.unlock();
    }

    public void setMediaData(BookInfo currentBook, MediaPlayer mp) {
        lock.lock();
        if (currentBook == null || mp == null) {
            prevPosition = -1;
        }
        this.book = currentBook;
        this.mediaPlayer = mp;
        if (currentBook != null && mp != null && prevPosition == -1) {
            prevPosition = calculatePosition();
        }
        lock.unlock();
    }

    private int calculatePosition() {
        long pos=0;
        for (int i = 0; i<book.getChapter(); i++) {
            pos+=book.getChapterDurations().get(i);
        }
        pos+=(mediaPlayer.getCurrentPosition()/1000);
        return (int) pos;
    }

    @Override
    public void complete(String name) {
        timerHandler.postDelayed(this, 1000);
    }
}
