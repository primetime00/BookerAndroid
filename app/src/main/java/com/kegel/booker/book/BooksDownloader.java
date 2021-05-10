package com.kegel.booker.book;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.kegel.booker.utils.InputStreamVolleyRequest;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BooksDownloader {
    private List<BookInfo> newBooks = null;
    private Context context;
    private Set<String> bookSet = new HashSet<>();
    private OnBooksComplete done;

    public interface OnBooksComplete {
        void complete();
    }

    public BooksDownloader(Context context,  List<BookInfo> newBooks) {
        this.newBooks = newBooks;
        this.context = context;
    }

    public void download(OnBooksComplete done) throws MalformedURLException, URISyntaxException {
        this.done = done;
        if (newBooks == null || newBooks.size() == 0) {
            done.complete();
            return;
        }
        for (BookInfo book : newBooks) {
            downloadBook(book);
        }
    }

    private void downloadBook(BookInfo book) throws MalformedURLException, URISyntaxException {
        int index = 0;
        RequestQueue queue = Volley.newRequestQueue(context);
        BookDownloadHandler handler = new BookDownloadHandler(book.getChapterFiles().size()) {
            @Override
            public void bookDownloadComplete() {
                downloadComplete(book);
            }
        };
        for (String chapterFile : book.getChapterFiles()) {
            String url = Helpers.getURL(context, String.format("book/%s/%d", book.getCrc(), index));
            String filename = Helpers.getName(book, chapterFile);
            InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, url, context, filename, handler, null);
            queue.add(request);
            index++;
        }
    }

    private void downloadComplete(BookInfo book) {
        Helpers.addBook(context, book);
        Log.d("book", String.format("Downloaded book %s", book.getTitle()));
        bookSet.add(book.getCrc());
        if (bookSet.size() >= newBooks.size()) { //we are done!
            if (done != null) {
                Log.d("book", "Done downloading all books");
                done.complete();
            }
        }
    }

}
