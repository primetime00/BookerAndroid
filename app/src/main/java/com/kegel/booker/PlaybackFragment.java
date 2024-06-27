package com.kegel.booker;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.kegel.booker.book.BookInfo;
import com.kegel.booker.book.Helpers;
import com.kegel.booker.media.BookPlayerService;
import com.kegel.booker.ui.FastForwardButton;
import com.kegel.booker.ui.PlayPauseButton;
import com.kegel.booker.ui.RewindButton;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PlaybackFragment extends Fragment {

    private MediaBrowserCompat mediaBrowser;
    private MediaControllerCompat mediaController;
    private PlaybackStateCompat lastPlaybackState;
    private MediaMetadataCompat lastMetadata;
    private final Handler timerHandler = new Handler();

    private PlayPauseButton playPauseButton;
    private RewindButton rewindButton;
    private FastForwardButton fastForwardButton;
    private long currentDurationMS;
    private BookInfo currentBook;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private final Runnable updateProgressTask = this::updateProgress;

    private ScheduledFuture<?> scheduleFuture;



    private MediaBrowserCompat.ConnectionCallback mediaBrowserCallback = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
            super.onConnected();
            mediaController = new MediaControllerCompat(getContext(), mediaBrowser.getSessionToken());
            mediaController.registerCallback(mediaControlCallback);
            MediaControllerCompat.setMediaController(getActivity(), mediaController);
            loadBook();
        }
    };

    private MediaControllerCompat.Callback mediaControlCallback = new MediaControllerCompat.Callback() {

        @Override
        public void onQueueChanged(List<MediaSessionCompat.QueueItem> queue) {
            super.onQueueChanged(queue);
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
            if( state == null ) {
                return;
            }
            lastPlaybackState = state;
            Log.d("ryan", "SET PLAYBACK STATE TO " + state);
            switch (state.getState()) {
                case PlaybackStateCompat.STATE_PLAYING: {
                    playPauseButton.setToPause(getView());
                    startTimer();
                    break;
                }
                case PlaybackStateCompat.STATE_PAUSED: {
                    enableMediaButtons(true);
                    playPauseButton.setToPlay(getView());
                    //updateProgress();
                    updatePlayerPosition(state.getPosition());
                    break;
                }
                case PlaybackStateCompat.STATE_BUFFERING:
                    enableMediaButtons(false);
                    break;
                case PlaybackStateCompat.STATE_CONNECTING:
                    break;
                case PlaybackStateCompat.STATE_ERROR:
                    break;
                case PlaybackStateCompat.STATE_FAST_FORWARDING:
                    break;
                case PlaybackStateCompat.STATE_NONE:
                    break;
                case PlaybackStateCompat.STATE_REWINDING:
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_QUEUE_ITEM:
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    break;
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
            lastMetadata = metadata;
            currentDurationMS = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            Log.d("ryan", String.format("Duration = %d", currentDurationMS));
            long chapter = metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER);

            //book title
            Log.d("book", String.format("Book metadata title is %s", metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE)));
            TextView txt = getActivity().findViewById(R.id.book_title);
            txt.setText(metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE));

            //book subtitle
            txt = getActivity().findViewById(R.id.chapter_view);
            txt.setText(getString(R.string.chapter) + ": " + metadata.getString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE));

            ImageView view = getActivity().findViewById(R.id.book_image);
            view.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART));
            updateProgress();
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            super.onSessionEvent(event, extras);
            if (event.equals(BookInfo.BOOK_COMPLETE)) {
                currentBook = Helpers.completeBook(currentBook, true, (AppCompatActivity) getActivity());
                Helpers.recordBook(getContext(), currentBook);
                try {
                    //bookCheckIn.process(currentBook);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private void startTimer() {
        if (!executorService.isShutdown()) {
            if (scheduleFuture == null) {
                scheduleFuture = executorService.scheduleAtFixedRate(
                        () -> timerHandler.post(updateProgressTask), 1000,
                        500, TimeUnit.MILLISECONDS);
            }
        }
    }

    private boolean hasHours(long timeMS) {
        return (int)(timeMS / (60000*60)) > 0;
    }

    private String getTimeString(long timeMS) {
        int seconds = (int) (timeMS / 1000);
        int minutes = (int) (timeMS / 60000);
        int hours = (int) (timeMS / (60000*60));
        if (hours > 0) {
            minutes = minutes % 60;
            seconds = seconds % 60;
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }
        else {
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Helpers.updateCurrentBook(getContext());

        //bookCheckIn = new BookCheckIn(getContext());

        Helpers.setSyncListener(this, () -> {
            currentBook = null;
            loadBook();
        });

        mediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(), BookPlayerService.class),
                mediaBrowserCallback, getActivity().getIntent().getExtras());

    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("playBackState", lastPlaybackState);
        //outState.putParcelable("metaData", lastMetadata);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playPauseButton = new PlayPauseButton(R.id.playStopButton, getContext(), android.R.drawable.ic_media_play, android.R.drawable.ic_media_pause);
        rewindButton = new RewindButton(R.id.rewindButton, getContext(), android.R.drawable.ic_media_rew);
        fastForwardButton = new FastForwardButton(R.id.fastforwardButton, getContext(), android.R.drawable.ic_media_ff);

        setPlayClick(playPauseButton);
        setFastForwardClick(fastForwardButton);
        setRewindClick(rewindButton);


        enableMediaButtons(false);


        if (!mediaBrowser.isConnected()) {
            mediaBrowser.connect();
        }
        else if (currentBook == null) { //we performed a sync, we need to reload a book
            loadBook();
        }
        else {
            if (lastMetadata != null) {
                mediaControlCallback.onMetadataChanged(lastMetadata);
            }
            if (lastPlaybackState != null) {
                mediaControlCallback.onPlaybackStateChanged(lastPlaybackState);
            }
            updateProgress();
            enableMediaButtons(true);
        }

        if (Helpers.getCurrentBook(getContext()).isEmpty()) {
            TextView txt = getActivity().findViewById(R.id.book_title);
            txt.setText(R.string.no_book);
            ImageView v = getActivity().findViewById(R.id.book_image);
            v.setVisibility(View.INVISIBLE);
            getActivity().findViewById(R.id.chapter_view).setVisibility(View.INVISIBLE);
            getActivity().findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            getActivity().findViewById(R.id.progress_text).setVisibility(View.INVISIBLE);
        }
    }

    private void setPlayClick(PlayPauseButton playPauseButton) {
        playPauseButton.setOnClickListener(getView(), v -> {
            PlayPauseButton.State state = playPauseButton.getState();
            switch (state) {
                case PLAY:
                    MediaControllerCompat.getMediaController(getActivity()).getTransportControls().pause();
                    break;
                case PAUSE: //at pause, now i want to play
                    MediaControllerCompat.getMediaController(getActivity()).getTransportControls().play();
                    break;
            }
        });
    }

    private void setRewindClick(RewindButton rewindButton) {
        rewindButton.setOnClickListener(getView(), v -> {
            mediaController.getTransportControls().rewind();
        });
    }

    private void setFastForwardClick(FastForwardButton rewindButton) {
        rewindButton.setOnClickListener(getView(), v -> {
            mediaController.getTransportControls().fastForward();
        });
    }


    private void enableMediaButtons(boolean enable) {
        playPauseButton.enable(getView(), enable);
        rewindButton.enable(getView(), enable);
        fastForwardButton.enable(getView(), enable);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (MediaControllerCompat.getMediaController(getActivity()) != null &&  MediaControllerCompat.getMediaController(getActivity()).getPlaybackState() != null) {
            if (MediaControllerCompat.getMediaController(getActivity()).getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().pause();
            }
        }
        executorService.shutdown();
        mediaBrowser.disconnect();
    }

    private void loadBook() {
        BookInfo book = Helpers.findCurrentBook(getContext());
        if (book != null) {
            currentBook = book;
            //if the book we are loading is complete, lets return to the library
            if (currentBook.isComplete()) {
                Helpers.completeBook(currentBook, true, (AppCompatActivity) getActivity());
                return;
            }
            Bundle bookInfo = new Bundle();
            bookInfo.putString(BookInfo.KEY_JSON, book.toString());
            Log.d("book", String.format("Loading book %s", book.getTitle()));
            MediaControllerCompat.getMediaController(getActivity()).getTransportControls().prepareFromMediaId(String.valueOf(book.getChapter()), bookInfo);
        }
    }

    private void updatePlayerPosition(long ms) {
        //check availability
        if (getActivity().findViewById(R.id.progress_text) == null)
            return;
        StringBuilder builder = new StringBuilder();
        if (hasHours(currentDurationMS) && !hasHours(ms)) {
            builder.append("00:");
        }
        builder.append(getTimeString(ms));
        builder.append(" / ");
        builder.append(getTimeString(currentDurationMS));
        TextView txt = getActivity().findViewById(R.id.progress_text);
        txt.setText(builder.toString());

        ProgressBar pb = getActivity().findViewById(R.id.progressBar);
        pb.setMax((int) (currentDurationMS/1000));
        pb.setProgress((int) (ms/1000));
    }

    private void updateProgress() {
        if (lastPlaybackState == null) {
            return;
        }
        long currentPosition = lastPlaybackState.getPosition();
        if (lastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {
            long timeDelta = SystemClock.elapsedRealtime() -
                    lastPlaybackState.getLastPositionUpdateTime();
            currentPosition += (int) timeDelta * lastPlaybackState.getPlaybackSpeed();
            if (currentPosition >= currentDurationMS)
                currentPosition = currentDurationMS;
        }
        /*
        if (Helpers.bookNeedsUpdate(currentBook, currentPosition, lastMetadata)) {
            //currentBook = Helpers.updateBookPosition(currentBook, currentPosition, lastMetadata);
            //Helpers.recordBook(getContext(), currentBook);
            try {
                //bookCheckIn.process(currentBook);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
        updatePlayerPosition(currentPosition);
    }

    public boolean hasBook() {
        return currentBook != null;
    }

    public boolean isBookComplete() {
        return currentBook == null || currentBook.isComplete();
    }




}