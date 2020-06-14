package com.jabin.mybk.sdk;

import android.view.View;

public abstract class BeBouncingOnClickListener implements View.OnClickListener {

    @Override
    public void onClick(View v) {
        doClick(v);
    }

    public abstract void doClick(View v);
}
