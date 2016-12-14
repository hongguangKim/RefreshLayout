package com.example.widget;

import android.content.Context;

/**
 * Created by AItsuki on 2016/6/13.
 *
 */
public interface IRefreshFooter {
    void reset();
    void refreshing();
    void init(Context context);
}
