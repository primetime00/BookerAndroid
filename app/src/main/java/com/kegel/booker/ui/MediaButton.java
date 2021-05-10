package com.kegel.booker.ui;

import android.content.Context;
import android.view.View;
import android.widget.ImageButton;

public class MediaButton {
    protected int id;
    protected Context context;
    boolean enabled;
    int drawable;

    public MediaButton(int id, Context context) {
        this.id = id;
        this.context = context;
        this.drawable = 0;
        this.enabled = true;
    }

    public MediaButton(int id, Context context, int drawable) {
        this.id = id;
        this.context = context;
        this.drawable = drawable;
        this.enabled = true;
    }

    public ImageButton getImageButton(View v) {
        return v.findViewById(id);
    }

    public void setOnClickListener(View v, View.OnClickListener listener) {
        getImageButton(v).setOnClickListener(listener);
    }

    public void enable(View v, boolean enable) {
        if (drawable > 0) {
            UIHelpers.setImageButtonEnabled(context, enable, getImageButton(v), drawable);
            this.enabled = enable;
        }
    }


}
