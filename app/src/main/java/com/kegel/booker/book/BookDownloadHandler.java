package com.kegel.booker.book;

import android.util.Log;

import com.kegel.booker.utils.OnDownloadDone;

import java.util.HashSet;
import java.util.Set;

public abstract class BookDownloadHandler implements OnDownloadDone {
    private int numberOfChapters;
    private Set<String> chapterSet = new HashSet<>();

    public BookDownloadHandler(int chapters) {
        this.numberOfChapters = chapters;
    }

    @Override
    public void complete(String name) {
        chapterSet.add(name);
        Log.d("book", String.format("Downloaded chapter %s", name));
        if (chapterSet.size() == numberOfChapters) {
            bookDownloadComplete();
        }
    }

    public abstract void bookDownloadComplete();
}
