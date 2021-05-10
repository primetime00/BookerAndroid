package com.kegel.booker.ui;

import android.content.Context;
import android.view.View;

public class PlayPauseButton extends MediaButton {
    private int drawablePlay;
    private int drawablePause;

    public enum State {
        PLAY,
        PAUSE
    };
    private State state = State.PAUSE;

    public PlayPauseButton(int id, Context c, int drawablePlay, int drawablePause) {
        super(id, c);
        this.drawablePlay = drawablePlay;
        this.drawablePause = drawablePause;
    }

    @Override
    public void enable(View v, boolean enable) {
        UIHelpers.setImageButtonEnabled(context, enable, getImageButton(v), getDrawable());
        this.enabled = enable;
    }

    public int getDrawable() {
        switch (state) {
            default:
            case PAUSE:
                return drawablePlay;
            case PLAY:
                return drawablePause;
        }
    }

    public State getState() {
        return state;
    }

    public void setToPause(View v) {
        state = State.PLAY;
        UIHelpers.setImageButtonEnabled(context, this.enabled, getImageButton(v), getDrawable());
    }

    public void setToPlay(View v) {
        state = State.PAUSE;
        UIHelpers.setImageButtonEnabled(context, this.enabled, getImageButton(v), getDrawable());
    }



}
