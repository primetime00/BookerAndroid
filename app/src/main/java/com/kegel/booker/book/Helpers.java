package com.kegel.booker.book;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentResultListener;
import androidx.preference.PreferenceManager;

import com.kegel.booker.PlaybackFragment;
import com.kegel.booker.R;
import com.kegel.booker.media.OnBookCompleteListener;
import com.kegel.booker.media.OnSyncListener;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class Helpers {

    public static List<BookInfo> getBooks(Context context) {
        List<BookInfo> books = new ArrayList<>();
        SharedPreferences bookPrefs = context.getSharedPreferences(context.getString(R.string.booker_books), Context.MODE_PRIVATE);
        bookPrefs.getAll().forEach((key, val) -> {
           String json = (String) val;
           books.add(BookInfo.create(json));
        });
        Comparator<BookInfo> comparator = Comparator.comparing(BookInfo::isComplete).thenComparing(BookInfo::getTitle);
        Collections.sort(books, comparator);
        return books;
    }

    public static void addBook(Context context, BookInfo book) {
        SharedPreferences bookPrefs = context.getSharedPreferences(context.getString(R.string.booker_books), Context.MODE_PRIVATE);
        bookPrefs.edit().putString(book.getCrc(), book.toString()).apply();
    }

    public static void removeBook(Context context, BookInfo book) {
        SharedPreferences bookPrefs = context.getSharedPreferences(context.getString(R.string.booker_books), Context.MODE_PRIVATE);
        removeBookFiles(context, book);
        bookPrefs.edit().remove(book.getCrc()).apply();
    }

    private static void removeBookFiles(Context context, BookInfo book) {
        for (String chapter : book.getChapterFiles()) {
            context.deleteFile(getName(book, chapter));
        }
    }

    public static String getName(BookInfo book, String chapterFile) {
        return String.format("%s_%s", book.getCrc(), chapterFile);
    }

    public static String getName(BookInfo book, int chapterNumber) {
        if (chapterNumber >= book.getChapterFiles().size())
            chapterNumber = book.getChapterFiles().size()-1;
        return String.format("%s_%s", book.getCrc(), book.getChapterFiles().get(chapterNumber));
    }


    public static String getURL(Context c, String path) throws URISyntaxException, MalformedURLException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        URI uri = new URI(prefs.getString(c.getString(R.string.booker_preference_url), ""));
        URL originalURL = uri.toURL();
        URL url = new URL(originalURL.getProtocol(), uri.getHost(), Integer.parseInt(prefs.getString(c.getString(R.string.booker_preference_port), "0")), path);
        return url.toString();
    }

    public static String getURL(Context c) throws URISyntaxException, MalformedURLException {
        return getURL(c, "");
    }

    public static void setBooks(Context context, List<BookInfo> newBookList) {
        SharedPreferences bookPrefs = context.getSharedPreferences(context.getString(R.string.booker_books), Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = bookPrefs.edit();
        edit.clear();
        for (BookInfo book : newBookList) {
            edit.putString(book.getCrc(), book.toString());
        }
        edit.apply();
    }

    public static String getCurrentBook(Context context) {
        SharedPreferences bookPrefs = context.getSharedPreferences(context.getString(R.string.booker_current_book), Context.MODE_PRIVATE);
        String crc = bookPrefs.getString(context.getString(R.string.booker_current_book), "");
        if (!currentBookValid(context, crc)) {
            return "";
        }
        return crc;
    }

    private static boolean currentBookValid(Context context, String crc) {
        List<BookInfo> books =  getBooks(context);
        return (books.stream().anyMatch(e -> e.getCrc().equals(crc)));
    }

    public static void setCurrentBook(Context context, BookInfo book) {
        SharedPreferences bookPrefs = context.getSharedPreferences(context.getString(R.string.booker_current_book), Context.MODE_PRIVATE);
        bookPrefs.edit().putString(context.getString(R.string.booker_current_book), book.getCrc()).apply();
    }

    public static void recordBook(Context context, BookInfo book) {
        SharedPreferences bookPrefs = context.getSharedPreferences(context.getString(R.string.booker_books), Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = bookPrefs.edit();
        edit.putString(book.getCrc(), book.toString());
        edit.apply();
    }

    public static void updateCurrentBook(Context context) {
        List<BookInfo> allBooks = Helpers.getBooks(context);
        String currentBook = Helpers.getCurrentBook(context);
        if (currentBook.isEmpty() && allBooks.size() > 0) {
            Helpers.setCurrentBook(context, allBooks.get(0));
        }
    }

    public static void removeBooks(Context context) {
        for (BookInfo book : getBooks(context)) {
            Helpers.removeBook(context, book);
        }
        SharedPreferences bookPrefs = context.getSharedPreferences(context.getString(R.string.booker_current_book), Context.MODE_PRIVATE);
        bookPrefs.edit().clear().apply();
    }

    public static BookInfo findCurrentBook(Context context) {
        String crc = getCurrentBook(context);
        if (crc.isEmpty())
            return null;
        Optional<BookInfo> info = getBooks(context).stream().filter(e -> e.getCrc().equals(crc)).findFirst();
        return info.orElse(null);
    }

    public static String getBookURI(Context context, BookInfo book) {
        String name = getName(book, book.getChapter());
        String path = String.format("%s/%s", context.getFilesDir().getPath(), name);
        return path;
    }

    public static List<String> getBookURIs(Context context, BookInfo book) {
        List<String> uris = new ArrayList<>();
        for (int i=0; i<book.getChapterFiles().size(); ++i) {
            String name = getName(book, i);
            String path = String.format("%s/%s", context.getFilesDir().getPath(), name);
            uris.add(path);
        }
        return uris;
    }

    public static List<MediaSessionCompat.QueueItem> bookToQueue(Context context,  BookInfo book) {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        List<String> uris = getBookURIs(context, book);
        for (int i=0; i<book.getChapterFiles().size(); ++i) {
            MediaSessionCompat.QueueItem qi = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                    .setMediaUri(Uri.fromFile(new File(uris.get(i))))
                    .setMediaId(String.valueOf(i))
                    .setTitle(book.getTitle())
                    .setSubtitle(context.getString(R.string.chapter) + " " + i)
                    .build(), i);
            queue.add(qi);
            }
        return queue;
    }

    public static BookInfo nextChapter(BookInfo book) {
        BookInfo.Builder builder = new BookInfo.Builder(book);
        builder.setChapter(book.getChapter()+1);
        return builder.build();
    }

    public static BookInfo setChapter(BookInfo book, int chapter) {
        BookInfo.Builder builder = new BookInfo.Builder(book);
        builder.setChapter(chapter);
        return builder.build();
    }

    public static boolean bookNeedsUpdate(BookInfo currentBook, long currentPosition, MediaMetadataCompat lastMetadata) {
        long chapter = lastMetadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
        long bookPosition = currentBook.getPosition()*1000;
        return (currentBook.getChapter() != chapter || Math.abs(bookPosition - currentPosition) >= 11000);

    }

    public static BookInfo updateBookPosition(BookInfo currentBook, long currentPosition, MediaMetadataCompat lastMetadata) {
        long chapter = lastMetadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);
        return new BookInfo.Builder(currentBook).setPosition(currentPosition/1000).setChapter((int) chapter).build();
    }

    public static void markSync(FragmentManager fm) {
        Bundle res = new Bundle();
        res.putBoolean("syncComplete", true);
        fm.setFragmentResult("sync", res);
    }

    public static void setSyncListener(PlaybackFragment fragment, OnSyncListener listener) {
        fragment.getParentFragmentManager().setFragmentResultListener("sync", fragment, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getBoolean("syncComplete", false)) {
                    listener.onSync();
                }
            }
        });
    }

    public static void setBookCompleteListener(AppCompatActivity activity, OnBookCompleteListener listener) {
        activity.getSupportFragmentManager().setFragmentResultListener("complete", activity, new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (result.getBoolean("complete", false)) {
                    listener.onComplete();
                }
            }
        });
    }


    public static BookInfo completeBook(BookInfo currentBook, boolean complete, AppCompatActivity activity) {
        BookInfo b = new BookInfo.Builder(currentBook).setComplete(complete).build();
        if (activity != null) {
            Bundle res = new Bundle();
            res.putBoolean("complete", true);
            activity.getSupportFragmentManager().setFragmentResult("complete", res);
        }
        return b;
    }

    public static int getBookProgressPercent(BookInfo bookInfo) {
        long totalPos = 0;
        int ch = bookInfo.getChapter();
        long pos = bookInfo.getPosition();
        for (int i=0; i<ch; ++i) {
            totalPos += bookInfo.getChapterDurations().get(i);
        }
        totalPos += pos;
        return  (int)(100 * (double)totalPos / bookInfo.getDuration());
    }

    public static BookInfo recordBook(Context context, BookInfo book, int currentPosition) {
        BookInfo b = new BookInfo.Builder(book).setPosition(currentPosition/1000).build();
        recordBook(context, b);
        return b;
    }

    public static boolean exists(Context context, BookInfo cBook) {
        for (int i=0; i<cBook.getChapterFiles().size(); ++i) {
            String fname = Helpers.getName(cBook, i);
            File f = new File(context.getFilesDir(), fname);
            if (!f.exists()) {
                Log.d("ryan", String.format("Cannot find file %s", fname));
                return false;
            }
        }
        return true;
    }
}
