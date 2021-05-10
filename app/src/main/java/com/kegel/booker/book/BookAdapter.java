package com.kegel.booker.book;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kegel.booker.R;

import java.util.List;

public class BookAdapter extends RecyclerView.Adapter<BookViewHolder> {

    private Context context;
    private List<BookInfo> bookList;
    private String currentCrc;
    private View.OnClickListener clickListener;

    public BookAdapter(Context context, View.OnClickListener listener) {
        this.context = context;
        bookList = Helpers.getBooks(context);
        currentCrc = Helpers.getCurrentBook(context);
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public BookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.book_row, parent, false);
        return new BookViewHolder(view, clickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull BookViewHolder holder, int position) {
        BookInfo bookInfo = bookList.get(position);
        holder.setBookTitle(bookInfo.getTitle());
        if (bookInfo.isComplete()) {
            holder.setBookProgress(context.getString(R.string.complete));
        }
        else {
            int bookPercent = Helpers.getBookProgressPercent(bookInfo);
            holder.setBookProgress(context.getString(R.string.chapter) + " " + String.format("%d / %d  (%d%%)", bookInfo.getChapter() + 1, bookInfo.getChapterFiles().size(), bookPercent));
        }
        if (bookInfo.getCrc().equals(currentCrc)) {
            holder.setCurrent(true);
        }
        else {
            holder.setCurrent(false);
        }
    }

    @Override
    public int getItemCount() {
        return bookList != null ? bookList.size() : 0;
    }
}
