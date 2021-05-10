package com.kegel.booker.book;

import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kegel.booker.R;

public class BookViewHolder extends RecyclerView.ViewHolder {
    private TextView bookName;
    private TextView bookProgress;
    private View.OnClickListener clickListener;

    public BookViewHolder(@NonNull View itemView, View.OnClickListener listener) {
        super(itemView);
        this.clickListener = listener;
        itemView.setOnClickListener(listener);
        bookName = itemView.findViewById(R.id.book_row_title);
        bookProgress = itemView.findViewById(R.id.book_row_progress);
    }



    public void setBookTitle(String title) {
        bookName.setText(title);
    }

    public void setBookProgress(String progress) {
        bookProgress.setText(progress);
    }


    public void setCurrent(boolean current) {
        bookName.setTypeface(bookName.getTypeface(), current ? Typeface.BOLD : Typeface.NORMAL);
    }

}
