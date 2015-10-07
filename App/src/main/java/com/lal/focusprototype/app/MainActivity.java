package com.lal.focusprototype.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.lal.focusprototype.app.views.CardStackView;
import com.lal.focusprototype.app.views.FeedItemView;

import org.apache.commons.lang3.StringUtils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    @InjectView(R.id.mCardStack)
    CardStackView mCardStack;

    @InjectView(R.id.card_add)
    Button mCardAdd;

    private Handler handler;
    private FeedListAdapter feedListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR);

        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        handler = new Handler();

        handler.postDelayed(new Runnable() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
            @Override
            public void run() {
                final View splash = findViewById(R.id.splash);

                splash.animate().alpha(0.0f).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        splash.setVisibility(View.GONE);
                        doInitialize();
                    }
                }).setDuration(2000).start();

            }
        }, 0);

        mCardStack.setCardStackListener(new CardStackView.CardStackListener() {
            @Override
            public void onUpdateProgress(boolean choice, float percent, View view) {
                FeedItemView item = (FeedItemView)view;
                item.onUpdateProgress(choice, percent, view);
            }

            @Override
            public void onCancelled(View beingDragged) {

                FeedItemView item = (FeedItemView)beingDragged;
                if (item != null)
                    item.onCancelled(beingDragged);

                // Dispatch cancel event to card that is being dragged == top card
                long downTime = SystemClock.uptimeMillis();
                long eventTime = SystemClock.uptimeMillis() + 100;
                float x = 0.0f;
                float y = 0.0f;
                int metaState = 0;
                MotionEvent motionEvent = MotionEvent.obtain(
                        downTime,
                        eventTime,
                        MotionEvent.ACTION_CANCEL,
                        x,
                        y,
                        metaState
                );
                if (beingDragged!=null)
                    beingDragged.dispatchTouchEvent(motionEvent);
            }

            @Override
            public void onChoiceMade(boolean choice, View beingDragged) {
                FeedItemView item = (FeedItemView)beingDragged;
                item.onChoiceMade(choice, beingDragged);
            }
        });

        return;
    }

    private void doInitialize() {
        feedListAdapter = new FeedListAdapter(this);
        mCardStack.setAdapter(feedListAdapter);
    }

    @OnClick(R.id.card_add)
    public void addCard() {

        // this part works when STACK_SIZE is visible
        feedListAdapter.addItemToBottom();

        // This part is to handle the scenario when visible item(s)
        // are less than STACK_SIZE.
        // It will not affect feedListAdapter.addItemToBottom();
        mCardStack.updateStack();
    }


    public Rect locateView(View view) {

        Rect loc = new Rect();
        int[] location = new int[2];
        if (view == null) {
            return loc;
        }
        view.getLocationOnScreen(location);

        loc.left = location[0];
        loc.top = location[1];
        loc.right = loc.left + view.getWidth();
        loc.bottom = loc.top + view.getHeight();
        return loc;
    }

}
