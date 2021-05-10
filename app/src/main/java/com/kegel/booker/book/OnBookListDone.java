package com.kegel.booker.book;

import java.util.List;

public interface OnBookListDone {
    void complete(List<BookInfo> books);
}
