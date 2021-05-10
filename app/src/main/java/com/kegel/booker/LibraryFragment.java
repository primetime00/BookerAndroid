package com.kegel.booker;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kegel.booker.book.BookAdapter;
import com.kegel.booker.book.BookInfo;
import com.kegel.booker.book.Helpers;

import java.util.List;

public class LibraryFragment extends Fragment implements View.OnClickListener {

    List<BookInfo> books;
    BookInfo selectedBook;
    String currentBook;

    DialogInterface.OnClickListener restartClickListener = (dialog, which) -> {
        switch (which){
            case DialogInterface.BUTTON_POSITIVE:
                ((AppCompatActivity)getActivity()).onSupportNavigateUp();
                selectedBook = new BookInfo.Builder(selectedBook)
                        .setChapter(0)
                        .setComplete(false)
                        .setPosition(0)
                        .build();
                Helpers.recordBook(getContext(), selectedBook);
                Helpers.setCurrentBook(getContext(), selectedBook);
                Helpers.markSync(getParentFragmentManager());
                break;

            case DialogInterface.BUTTON_NEGATIVE:
                break;
        }
    };

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        books = Helpers.getBooks(getContext());
        currentBook = Helpers.getCurrentBook(getContext());
        RecyclerView rv = view.findViewById(R.id.library_view);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new BookAdapter(getContext(), this));
    }

    @Override
    public void onClick(View v) {
        RecyclerView rv = getActivity().findViewById(R.id.library_view);
        int pos = rv.getChildAdapterPosition(v);
        selectedBook = books.get(pos);
        if (selectedBook.isComplete()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.restart_question).setPositiveButton(R.string.yes, restartClickListener)
            .setNegativeButton(R.string.no, restartClickListener).show();
            return;
        }

        if (!selectedBook.getCrc().equals(currentBook)) {
            Helpers.setCurrentBook(getContext(), selectedBook);
            Helpers.markSync(getParentFragmentManager());
        }
        ((AppCompatActivity)getActivity()).onSupportNavigateUp();
    }
}