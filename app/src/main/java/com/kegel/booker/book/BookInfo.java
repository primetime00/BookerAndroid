package com.kegel.booker.book;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BookInfo {
    private String crc;
    private int chapter;
    private List<String> chapterFiles;
    private List<Long> chapterDurations;
    private long position;
    private String title;
    private long duration;
    private boolean complete;

    public static String KEY_TITLE = "Title";
    public static String KEY_NUMBER_OF_CHAPTERS = "Chapters";
    public static String KEY_CHAPTER = "Chapter";
    public static String KEY_POSITION = "Position";
    public static String KEY_JSON = "JSON";
    public static final String BOOK_COMPLETE = "Complete";

    public BookInfo(JSONObject obj) throws JSONException {
        chapterFiles = new ArrayList<>();
        chapterDurations = new ArrayList<>();
        this.chapter = obj.getInt("chapter");
        this.crc = obj.getString("crc");
        this.position = obj.getInt("position");
        this.title = obj.getString("title");
        this.duration = obj.getLong("duration");
        this.complete = obj.getBoolean("complete");
        JSONArray array = obj.getJSONArray("chapters");
        for (int i=0; i<array.length(); ++i) {
            chapterFiles.add(array.getString(i));
        }
        array = obj.getJSONArray("chapterDurations");
        for (int i=0; i<array.length(); ++i) {
            chapterDurations.add(array.getLong(i));
        }

    }

    public static BookInfo create(String json) {
        JsonElement el = JsonParser.parseString(json);
        Gson gson = new Gson();
        return gson.fromJson(el, BookInfo.class);
    }

    private  BookInfo(String crc, int chapter, List<String> chapterFiles, long position, String title, long duration, boolean complete, List<Long> chapterDurations) {
        this.crc = crc;
        this.chapter = chapter;
        this.chapterFiles = chapterFiles;
        this.position = position;
        this.title = title;
        this.duration = duration;
        this.complete = complete;
        this.chapterDurations = chapterDurations;
    }

    public String getCrc() {
        return crc;
    }

    public int getChapter() {
        return chapter;
    }

    public List<String> getChapterFiles() {
        return chapterFiles;
    }

    public long getPosition() {
        return position;
    }

    public String getTitle() {
        return title;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isComplete() {
        return complete;
    }

    public List<Long> getChapterDurations() {
        return chapterDurations;
    }

    public void setChapterDurations(List<Long> chapterDurations) {
        this.chapterDurations = chapterDurations;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this, BookInfo.class);
    }

    public static class Builder {
        private String crc;
        private int chapter;
        List<String> chapterFiles;
        private long position;
        private String title;
        private long duration;
        private boolean complete;
        private List<Long> chapterDurations;

        public Builder(BookInfo book) {
            this.crc = book.crc;
            this.chapter = book.chapter;
            this.chapterFiles = book.chapterFiles;
            this.position = book.position;
            this.title = book.title;
            this.duration = book.duration;
            this.complete = book.complete;
            this.chapterDurations = book.chapterDurations;
        }

        public Builder setCrc(String crc) {
            this.crc = crc;
            return this;
        }

        public Builder setChapter(int chapter) {
            this.chapter = chapter;
            return this;
        }

        public Builder setChapterFiles(List<String> chapterFiles) {
            this.chapterFiles = chapterFiles;
            return this;
        }

        public Builder setChapterDurations(List<Long> chapterDurations) {
            this.chapterDurations = chapterDurations;
            return this;
        }


        public Builder setPosition(long position) {
            this.position = position;
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setDuration(long duration) {
            this.duration = duration;
            return this;
        }

        public Builder setComplete(boolean complete) {
            this.complete = complete;
            return this;
        }


        public BookInfo build() {
            return new BookInfo(crc, chapter, chapterFiles, position, title, duration, complete, chapterDurations);
        }
    }
}
