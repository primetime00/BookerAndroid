package com.kegel.booker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.kegel.booker.book.BookInfo;
import com.kegel.booker.book.BookUpdater;
import com.kegel.booker.book.BooksDownloader;
import com.kegel.booker.book.Helpers;
import com.kegel.booker.book.OnBookListDone;
import com.kegel.booker.utils.OnDownloadDone;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SyncFragment extends Fragment {

    private List<BookInfo> currentBooks;
    private List<BookInfo> newBooks = null;
    private List<BookInfo> oldBooks = null;
    private BooksDownloader downloader;
    private StringBuilder statusBuilder;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sync, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        currentBooks = Helpers.getBooks(getContext());
        startSync();
    }

    private void startSync() {
        ((TextView)getActivity().findViewById(R.id.sync_status)).setText("");
        statusBuilder = new StringBuilder();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        addStatus(getString(R.string.sync_status_connecting), " ", prefs.getString(getString(R.string.booker_preference_url), ""), ":", prefs.getString(getString(R.string.booker_preference_port), "0"));
        try {
            getBookList(books -> {
                //removeCurrentBooks();
                newBooks = findChangedBooks(books, true);
                oldBooks = findChangedBooks(books, false);
                removeBooks();
                downloader = new BooksDownloader(getContext(), newBooks);
                try {
                    downloader.download(() -> {
                        try {
                            updateBooks(status -> {
                                if (status.equals("SUCCESS")) {
                                    assignBook();
                                    Helpers.markSync(getParentFragmentManager());
                                    getActivity().findViewById(R.id.sync_progress_bar).setVisibility(View.INVISIBLE);
                                    addStatus(getString(R.string.sync_complete));
                                }
                                else {
                                    getActivity().findViewById(R.id.sync_progress_bar).setVisibility(View.INVISIBLE);
                                    addStatus(getString(R.string.sync_failed));
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            getActivity().findViewById(R.id.sync_progress_bar).setVisibility(View.INVISIBLE);
                            addStatus(getString(R.string.sync_failed));
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    getActivity().findViewById(R.id.sync_progress_bar).setVisibility(View.INVISIBLE);
                    addStatus(getString(R.string.sync_failed));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            getActivity().findViewById(R.id.sync_progress_bar).setVisibility(View.INVISIBLE);
            addStatus(getString(R.string.sync_failed));
        }


    }

    private void assignBook() {
        Helpers.updateCurrentBook(getContext());
    }

    private void updateBooks(OnDownloadDone downloadDone) throws MalformedURLException, URISyntaxException, JSONException {
        BookUpdater updater = new BookUpdater(getContext(), downloadDone);
        updater.process();

    }


    private void removeBooks() {
        for (BookInfo book : oldBooks) {
            Helpers.removeBook(getContext(), book);
        }
    }

    private List<BookInfo> findChangedBooks(List<BookInfo> downloadedBooks, boolean isNew) {
        List<BookInfo> changedBooks = new ArrayList<>();
        if (isNew) {
            for (BookInfo dBook : downloadedBooks) {
                boolean found = false;
                for (BookInfo cBook : currentBooks) {
                    if (cBook.getCrc().equals(dBook.getCrc())) { //we have this book, but do we have all of it?
                        found = Helpers.exists(getContext(), cBook);
                        break;
                    }
                }
                if (!found) { //don't have this downloaded book
                    changedBooks.add(dBook);
                }
            }
        }
        else {
            for (BookInfo cBook : currentBooks) {
                boolean found = false;
                for (BookInfo dBook : downloadedBooks) {
                    if (cBook.getCrc().equals(dBook.getCrc())) { //we have this book
                        found = true;
                        break;
                    }
                }
                if (!found) { //don't have this downloaded book
                    changedBooks.add(cBook);
                }
            }
        }
        return changedBooks;
    }

    private void getBookList(OnBookListDone bookDone) throws MalformedURLException, URISyntaxException {
        RequestQueue queue = Volley.newRequestQueue(getContext());
        String listURL = Helpers.getURL(getContext(), "list");
        List<BookInfo> books = new ArrayList<>();
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, listURL, null,
                response -> {
                    getActivity().findViewById(R.id.sync_progress_bar).setVisibility(View.VISIBLE);
                    addStatus(getString(R.string.syncing));
                    response.keys().forEachRemaining(key -> {
                        try {
                            BookInfo book = new BookInfo(response.getJSONObject(key));
                            books.add(book);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    });
                    bookDone.complete(books);
                }, (Response.ErrorListener) error -> {
                    addStatus(getString(R.string.cannot_connect));
                    //bookDone.complete(null);
                });

        queue.add(jsonRequest);
    }

    private void addStatus(String ... data) {
        Arrays.asList(data).stream().forEach(e -> statusBuilder.append(e));
        statusBuilder.append("\n");
        ((TextView)getActivity().findViewById(R.id.sync_status)).setText(statusBuilder.toString());
    }

}