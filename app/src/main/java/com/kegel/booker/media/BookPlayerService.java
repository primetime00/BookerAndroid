package com.kegel.booker.media;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.media.session.MediaButtonReceiver;

import com.kegel.booker.R;
import com.kegel.booker.book.BookCheckIn;
import com.kegel.booker.book.BookInfo;
import com.kegel.booker.book.Helpers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_QUEUE_COMMANDS;

public class BookPlayerService extends MediaBrowserServiceCompat implements MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener, MediaPlayer.OnPreparedListener {
    public static final String COMMAND_EXAMPLE = "command_example";

    private MediaSessionCompat mediaSession;
    private MediaPlayer mediaPlayer;
    private MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();

    private PlaybackStateCompat.Builder stateBuilder;
    private int FLAG_CURRENTLY_PLAYING = 1 << 1;
    private int FLAG_PREVIOUS_CHAPTER = 1 << 2;
    private int CHAPTER_FLAG_RESTART = 1 << 3;


    private BookInfo currentBook;
    private int startSeconds;
    private String currentMediaID = "0";
    private int chapterChangeFlag = 0;
    private BookCheckIn bookCheckIn;
    private boolean autoAudioLoss = false;


    private final int seekForwardSeconds = 30;
    private final int seekBackwardsSeconds = 10;

    private BroadcastReceiver noisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if( mediaPlayer != null && mediaPlayer.isPlaying() ) {
                mediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
            }
        }
    };


    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        @Override
        public void onPlay() {
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition());
            super.onPlay();
            if( !successfullyRetrievedAudioFocus() ) {
                return;
            }
            mediaSession.setActive(true);
            Log.d("ryan", String.format("starting playback at %d ms", mediaPlayer.getCurrentPosition()));
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition());
            showPlayingNotification();
            mediaPlayer.start();
        }



        @Override
        public void onFastForward() {
            super.onFastForward();
            setMediaPlaybackState(PlaybackStateCompat.STATE_FAST_FORWARDING, mediaPlayer.getCurrentPosition());
            int cp = mediaPlayer.getCurrentPosition();
            if (cp + (seekForwardSeconds *1000) >= mediaPlayer.getDuration()) {
                nextChapter(mediaPlayer);
            }
            else {
                translate(seekForwardSeconds);
            }

        }

        @Override
        public void onRewind() {
            super.onRewind();
            setMediaPlaybackState(PlaybackStateCompat.STATE_REWINDING, mediaPlayer.getCurrentPosition());
            int cp = mediaPlayer.getCurrentPosition();
            if (cp - (seekBackwardsSeconds *1000) < 0) {
                previousChapter(mediaPlayer, FLAG_PREVIOUS_CHAPTER);
            }
            else {
                translate(-seekBackwardsSeconds);
            }
        }

        @Override
        public void onPause() {
            super.onPause();

            if( mediaPlayer.isPlaying() ) {
                mediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
                showPausedNotification();
                bookCheckIn.postCheckIn();
            }
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            super.onPrepareFromMediaId(mediaId, extras);
            String currentID = Helpers.getCurrentBook(getApplicationContext());
            boolean reQueue = false;
            bookCheckIn.setMediaData(null, null);
            if (mediaSession.getController().getQueue() != null) {
                reQueue = !currentBook.getCrc().equals(currentID);
            }
            currentBook = BookInfo.create(extras.getString(BookInfo.KEY_JSON));

            if (mediaSession.getController().getQueue() == null || reQueue) {
                mediaSession.setQueue(Helpers.bookToQueue(BookPlayerService.this, currentBook));
            }

            int id = Integer.parseInt(mediaId);
            startSeconds = (int) currentBook.getPosition();
            loadChapter(mediaPlayer, id, 0);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            super.onRemoveQueueItem(description);
            List<MediaSessionCompat.QueueItem> items = mediaSession.getController().getQueue();
            if (items != null) {
                items.stream().filter(e -> e.getDescription().getMediaUri().getPath().equals(description.getMediaUri().getPath())).findAny().ifPresent(f -> items.remove(f));
                mediaSession.setQueue(items.size() > 0 ? items : null);
            }
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            super.onAddQueueItem(description, index);
            List<MediaSessionCompat.QueueItem> items = mediaSession.getController().getQueue();
            if (items == null) {
                items = new ArrayList<>();
            }
            items.add(new MediaSessionCompat.QueueItem(description, index));
            mediaSession.setQueue(items);


        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
            bookCheckIn.setMediaData(null, null);
            try {
                currentBook = BookInfo.create(extras.getString(BookInfo.KEY_JSON));
                setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING, currentBook.getPosition());
                mediaPlayer.setDataSource(uri.toString());
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                mediaPlayer.release();
                initMediaPlayer();
            }
        }



        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
            if( COMMAND_EXAMPLE.equalsIgnoreCase(command) ) {
                //Custom command here
            }
        }

        @Override
        public void onSeekTo(long pos) {
            Log.d("ryan", String.format("seeking to %d", pos));

            super.onSeekTo(pos);
        }

    };

    private void translate(int sec) {
        boolean needsPlayed = false;
        int pos = mediaPlayer.getCurrentPosition();
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            needsPlayed = true;
        }
        mediaPlayer.seekTo(pos+(sec*1000));
        if (needsPlayed) {
            mediaPlayer.start();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition());
        }
        else {
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
        }
    }

    private void initNoisyReceiver() {
        //Handles headphones coming unplugged. cannot be done through a manifest receiver
        IntentFilter filter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(noisyReceiver, filter);
    }



    @Override
    public void onCreate() {
        super.onCreate();

        bookCheckIn = new BookCheckIn(getApplicationContext());

        initMediaPlayer();
        initMediaSession();
        initNoisyReceiver();
        bookCheckIn.start();


        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_STOP |
                        PlaybackStateCompat.ACTION_PREPARE |
                        PlaybackStateCompat.ACTION_PLAY_FROM_URI |
                        PlaybackStateCompat.ACTION_PREPARE_FROM_URI |
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(this);
        unregisterReceiver(noisyReceiver);
        mediaSession.release();
        bookCheckIn.destroy();
        NotificationManagerCompat.from(this).cancel(1);
    }

    @Override
    public void onCustomAction(@NonNull String action, Bundle extras, @NonNull Result<Bundle> result) {
    }



    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(BookPlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setVolume(1.0f, 1.0f);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        AudioAttributes aa = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).setUsage(AudioAttributes.USAGE_MEDIA).build();
        mediaPlayer.setAudioAttributes(aa);
    }

    private void showPlayingNotification() {
        NotificationCompat.Builder builder = MediaStyleHelper.from(BookPlayerService.this, mediaSession);
        if( builder == null ) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.setStyle(new MediaStyle().setShowActionsInCompactView(0).setMediaSession(mediaSession.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        NotificationManagerCompat.from(BookPlayerService.this).notify(1, builder.build());
    }

    private void showPausedNotification() {
        NotificationCompat.Builder builder = MediaStyleHelper.from(this, mediaSession);
        if( builder == null ) {
            return;
        }

        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play, "Play", MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        builder.setStyle(new MediaStyle().setShowActionsInCompactView(0).setMediaSession(mediaSession.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        NotificationManagerCompat.from(this).notify(1, builder.build());
    }


    private void initMediaSession() {
        ComponentName mediaButtonReceiver = new ComponentName(BookPlayerService.this, MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(BookPlayerService.this, "Tag", mediaButtonReceiver, null);

        mediaSession.setFlags(FLAG_HANDLES_QUEUE_COMMANDS);
        mediaSession.setCallback(mediaSessionCallback);

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
        mediaSession.setMediaButtonReceiver(pendingIntent);

        setSessionToken(mediaSession.getSessionToken());
    }

    private void setMediaPlaybackState(int state) {
        setMediaPlaybackState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN);
    }

    private void setMediaPlaybackState(int state, long position) {
        PlaybackStateCompat.Builder playbackstateBuilder = new PlaybackStateCompat.Builder();
        if( state == PlaybackStateCompat.STATE_PLAYING ) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE);
        } else if (state == PlaybackStateCompat.STATE_BUFFERING) {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_STOP);
        } else {
            playbackstateBuilder.setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PLAY);
        }
        playbackstateBuilder.setState(state, position, 1);

        mediaSession.setPlaybackState(playbackstateBuilder.build());
    }

    private void initMediaSessionMetadata() {
        Log.d("book", String.format("have %s - %s", currentBook.getTitle(), metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)));
        String title = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) == null ? currentBook.getTitle() : metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String author = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) == null ? "" : metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String chapter = metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER) == null ? String.format("%d/%d", Integer.parseInt(currentMediaID), currentBook.getChapterFiles().size()) : metadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
        byte [] photo = metadataRetriever.getEmbeddedPicture();


        MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
        //Notification icon in card
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));


        //lock screen icon for pre lollipop
        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, photo == null ? BitmapFactory.decodeResource(getResources(), R.drawable.books) : BitmapFactory.decodeByteArray(photo, 0, photo.length)); //BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher));

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, chapter);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, Integer.parseInt(currentMediaID));
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, currentBook.getChapterFiles().size());
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer.getDuration());

        MediaMetadataCompat metadataCompat = metadataBuilder.build();

        mediaSession.setMetadata(metadataCompat);
    }

    private boolean successfullyRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        int result = audioManager.requestAudioFocus(this,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }


    //Not important for general audio service, required for class
    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    //Not important for general audio service, required for class
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (mediaPlayer == null)
            return;
        switch( focusChange ) {
            case AudioManager.AUDIOFOCUS_LOSS: {
                Log.d("audio", "AUDIO FOCUS LOSS");
                if( mediaPlayer.isPlaying() ) {
                    autoAudioLoss = true;
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
                    mediaPlayer.pause();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
            {
                Log.d("audio", "AUDIO FOCUS LOSS TRANSIENT/DUCK");
                if (mediaPlayer.isPlaying()) {
                    autoAudioLoss = true;
                    int pos = Math.max(mediaPlayer.getCurrentPosition() - 2000, 0);
                    mediaPlayer.seekTo(pos);
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
                    mediaPlayer.pause();
                }
                break;
            }
            case AudioManager.AUDIOFOCUS_GAIN: {
                Log.d("audio", "AUDIO FOCUS GAIN");
                if( !mediaPlayer.isPlaying() && autoAudioLoss) {
                    autoAudioLoss = false;
                    setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition());
                    mediaPlayer.start();
                }
                break;
            }
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        int id =  Integer.parseInt(currentMediaID) + 1;
        int size = mediaSession.getController().getQueue().size();
        if (id >= size){ //the book is done, restart
            mediaSession.sendSessionEvent(BookInfo.BOOK_COMPLETE, null);
            //loadChapter(mp, 0, 0);
        } else{
            loadChapter(mp, id, FLAG_CURRENTLY_PLAYING);
        }

    }

    private void loadChapter(MediaPlayer mp, int chapter, int flags) {
        mp.stop();
        chapterChangeFlag = flags;
        MediaSessionCompat.QueueItem item = mediaSession.getController().getQueue().get(chapter);
        setMediaPlaybackState(PlaybackStateCompat.STATE_BUFFERING, 0);
        bookCheckIn.setMediaData(null, null);
        try {
            mp.release();
            initMediaPlayer();
            mp = mediaPlayer;
            metadataRetriever.setDataSource(item.getDescription().getMediaUri().getPath());
            Log.d("book", String.format("Loading chapter URL %s", item.getDescription().getMediaUri().getPath()));
            mp.setDataSource(item.getDescription().getMediaUri().toString());
            currentMediaID = String.valueOf(chapter);
            currentBook = new BookInfo.Builder(currentBook).setChapter(chapter).build();
            Log.d("timer", "setting chapter to " + chapter);
            mp.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            mediaPlayer.release();
            initMediaPlayer();
        }
    }

    private void nextChapter(MediaPlayer mp) {
        int id =  Integer.parseInt(currentMediaID) + 1;
        int size = currentBook.getChapterFiles().size();
        if (id >= size) { //fast forwarded too much, pause the book near the end
            if (mp.isPlaying()) {
                mp.pause();
            }
            mp.seekTo(mp.getDuration() - 1000);
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mp.getCurrentPosition());
            return;
        }
        int flags = 0;
        if (mediaPlayer.isPlaying())
            flags |= FLAG_CURRENTLY_PLAYING;
        loadChapter(mp, id, flags);
    }

    private void previousChapter(MediaPlayer mp, int flags) {
        int id =  Integer.parseInt(currentMediaID) - 1;
        if (id < 0) {//we are at the beginning of our book.  Lets just seek to 0
            mp.seekTo(0);
            setMediaPlaybackState(mediaPlayer.isPlaying() ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
            return;
        }
        if (mediaPlayer.isPlaying())
            flags |= FLAG_CURRENTLY_PLAYING;
        loadChapter(mp, id, flags);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }



    @Override
    public void onPrepared(MediaPlayer mp) {
        mediaPlayer = mp;
        boolean wasPaused = (chapterChangeFlag & FLAG_CURRENTLY_PLAYING) == 0;
        boolean needsRestart = (chapterChangeFlag & CHAPTER_FLAG_RESTART) > 0;
        initMediaSessionMetadata();
        if (needsRestart) { //reached end of book or beginning
            mediaPlayer.seekTo(0);
            if (!wasPaused) {
                mediaPlayer.pause();
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
            }
            chapterChangeFlag = 0;
            return;
        }
        if ( !wasPaused ) { //we changed a chapter while playing
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            if ( (chapterChangeFlag & FLAG_PREVIOUS_CHAPTER) > 0 ) { //we rewound so seek back x seconds
                mediaPlayer.seekTo(mediaPlayer.getDuration() - (seekForwardSeconds * 1000));
            } else { //we either went to the next chapter
                mp.seekTo(0);
            }
            setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
        } else { //we changed a chapter while on pause
            if ( (chapterChangeFlag & FLAG_PREVIOUS_CHAPTER) > 0 ) { //we rewound so seek back x seconds
                mediaPlayer.seekTo(mediaPlayer.getDuration() - (seekForwardSeconds * 1000));
            } else { //we either went to the next chapter
                mediaPlayer.seekTo(0);
            }
        }
        if (startSeconds > 0) {
            mediaPlayer.seekTo(startSeconds*1000);
            startSeconds = 0;
        }
        int position = mediaPlayer.getCurrentPosition();

        if ( !wasPaused ) {
            mp.start();
            setMediaPlaybackState(PlaybackStateCompat.STATE_PLAYING, position);
        } else {
            if (mediaSession.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_PAUSED) {
                setMediaPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());
            }
        }
        bookCheckIn.setMediaData(currentBook, mp);
        chapterChangeFlag = 0;
    }

}
