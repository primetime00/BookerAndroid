package com.kegel.booker.book;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;

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

public class BookCheckIn implements OnDownloadDone {

    private Handler checkinHandler;
    private Handler recordHandler;

    private MediaPlayer mediaPlayer;
    private BookInfo book;

    private int prevCheckinPos = -1;
    private int prevRecordPos = -1;

    ReentrantLock lock = new ReentrantLock();

    private final Context context;

    private final Runnable checkinRunnable = new Runnable() {
        @Override
        public void run() {
            runCheckIn(true);
        }
    };
    private final Runnable recordRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                lock.lock();
                if (book == null || mediaPlayer == null) {
                    recordHandler.postDelayed(recordRunnable, 1000);
                    return;
                }
                int pos = calculatePosition();
                if (Math.abs(pos - prevRecordPos) >= 1) {
                    prevRecordPos = pos;
                    try {
                        Log.d("timer", "recording book position at ch: " + book.getChapter() + " pos " + mediaPlayer.getCurrentPosition()/1000);
                        Helpers.recordBook(context, book, mediaPlayer.getCurrentPosition());
                        recordHandler.postDelayed(recordRunnable, 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    recordHandler.postDelayed(recordRunnable, 1000);
                }
            }
            finally {
                lock.unlock();
            }
        }
    };


    public BookCheckIn(Context context) {
        this.context = context;
        this.checkinHandler = new Handler();
        this.recordHandler = new Handler();
    }

    public void checkInOnPause() {
        try {
            lock.lock();
            if (book == null || mediaPlayer == null) {
                return;
            }
            int pos = calculatePosition();
            prevCheckinPos = pos;
            Log.d("timer", "Saving book progress");
            try {
                process(book);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void runCheckIn(boolean repeat) {
        try {
            lock.lock();
            if (book == null || mediaPlayer == null) {
                if (repeat) {
                    checkinHandler.postDelayed(checkinRunnable, 1000);
                }
                return;
            }
            int pos = calculatePosition();
            if (Math.abs(pos - prevCheckinPos) > 60) {
                prevCheckinPos = pos;
                Log.d("timer", "Saving book progress");
                try {
                    process(book);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (repeat) {
                    checkinHandler.postDelayed(checkinRunnable, 1000);
                }
            }
        }
        finally {
            lock.unlock();
        }
    }

    public void process(BookInfo book) throws MalformedURLException, URISyntaxException {
        String url = Helpers.getURL(context, "checkin");
        RequestQueue queue = Volley.newRequestQueue(context);
        BookInfo recBook = Helpers.recordBook(context, book, mediaPlayer.getCurrentPosition());
        Log.d("timer", String.format("Bout to send checking at pos %d", recBook.getPosition()));

        StringRequest jsonRequest = new StringRequest(Request.Method.POST, url, response -> {
            Gson gson = new Gson();
            HashMap<String, Object> res = gson.fromJson(response, HashMap.class);
            complete("SUCCESS");
        }, error -> {
            complete("FAIL");
        }
        ) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> map = new HashMap<>();
                map.put("DEVICE", "PHONE");
                map.put(recBook.getCrc(), recBook.toString());
                return map;
            }
        };
        queue.add(jsonRequest);
    }

    public void start() {
        checkinHandler.postDelayed(checkinRunnable, 1100);
        recordHandler.postDelayed(recordRunnable, 500);

    }

    public void destroy() {
        checkinHandler.removeCallbacks(checkinRunnable);
        recordHandler.removeCallbacks(recordRunnable);
    }

    public void setMediaData(BookInfo currentBook, MediaPlayer mp) {
        lock.lock();
        if (currentBook == null || mp == null) {
            prevCheckinPos = -1;
            prevRecordPos = -1;
        }
        this.book = currentBook;
        this.mediaPlayer = mp;
        if (currentBook != null && mp != null && (prevCheckinPos == -1 || prevRecordPos == -1)) {
            prevCheckinPos = calculatePosition();
            prevRecordPos = prevCheckinPos;
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

    public void postCheckIn() {
        checkInOnPause();
    }

    @Override
    public void complete(String name) {
        checkinHandler.postDelayed(checkinRunnable, 1000);
    }
}
