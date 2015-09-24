package com.lal.focusprototype.app.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lal.focusprototype.app.CirclePageIndicator;
import com.lal.focusprototype.app.R;
import com.lal.focusprototype.app.VerticalViewPager;
import com.lal.focusprototype.app.VerticalViewPager2;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;


/**
 * Created by diallo on 14/04/14.
 *
 * An implementation of a tinder like cardstack that can be swiped left or right.
 * The implmentation use the http://nineoldandroids.com/ implentation to be compatible
 * with pre ICS.
 *
 */
public class CardStackView extends RelativeLayout {

    private static final float MIN_SCALE = 0.75f;
    private static final float MIN_ALPHA = 0.75f;

    private static final String TAG = "CardStackView";
    private boolean mMovingVerticallyMore;
    private ArrayList<Boolean> mMovingVertically = new ArrayList<>();
    int verticalMoreCount = 0;
    int verticalLessCount = 0;
    // private VerticalViewPager verticalViewPager;

    private Handler handler;

    public interface CardStackListener{
        void onUpdateProgress(boolean positif, float percent, View view);

        void onCancelled(View beingDragged);

        void onChoiceMade(boolean choice, View beingDragged);
    }

    private static int STACK_SIZE = 4;
    private static int MAX_ANGLE_DEGREE = -20;//20
    private BaseAdapter mAdapter;
    private int mCurrentPosition;
    private int mMinDragDistance;
    private int mMinAcceptDistance;

    private int mXDelta;
    private int mYDelta;

    protected LinkedList<View> mCards = new LinkedList<View>();
    protected LinkedList<View> mRecycledCards = new LinkedList<View>();


    private CardStackListener mCardStackListener;

    protected LinkedList<Object> mCardStack = new LinkedList<Object>();
    private int mXStart;
    private int mYStart;
    private View mBeingDragged;
    private MyOnTouchListener mMyTouchListener;

    public CardStackView(Context context) {
        super(context);
        setup();
    }

    public CardStackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup();
    }

    public CardStackView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup();
    }

    private void setup() {

        Resources r = getContext().getResources();
        mMinDragDistance = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());
        mMinAcceptDistance = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, r.getDisplayMetrics());

        if (isInEditMode()) {
            mAdapter = new MockListAdapter(getContext());
        }

        mCurrentPosition = 0;
    }

    public void setAdapter(BaseAdapter adapter) {
        mAdapter = adapter;
        mRecycledCards.clear();
        mCards.clear();
        removeAllViews();
        mCurrentPosition = 0;

        initializeStack();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        View card = mCards.getFirst();

        // this is how I pass the motion events to the listener registered on each card
        mMyTouchListener.onTouch(card, ev);

        return super.onInterceptTouchEvent(ev);
    }

    private void initializeStack() {
        int position = 0;
        for (; position < mCurrentPosition + STACK_SIZE;
             position++) {

            if (position >= mAdapter.getCount()) {
                break;
            }

            Object item = mAdapter.getItem(position);
            mCardStack.offer(item);
            View card = mAdapter.getView(position, null, null);

            mCards.offer(card);

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            addView(card, 0, params);

            mMyTouchListener = new MyOnTouchListener();

            initiateViewPager(card);
        }

        mCurrentPosition += position;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mBeingDragged != null){
            mXDelta = (int) mBeingDragged.getTranslationX();
            mYDelta = (int) mBeingDragged.getTranslationY();

        }

        int index = 0;
        Iterator<View> it = mCards.descendingIterator();
        while (it.hasNext()) {
            View card =  it.next();
            if (card == null) {
                break;
            }

            if (isTopCard(card)){
                card.setOnTouchListener(mMyTouchListener);
            }else{
                card.setOnTouchListener(null);
            }

            if (index == 0 && adapterHasMoreItems()) {
                if (mBeingDragged != null){
                    index++;
                    continue;
                }

                scaleAndTranslate(1, card);
            } else {
                scaleAndTranslate(index, card);
            }

            index++;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private boolean adapterHasMoreItems() {
        return mCurrentPosition < mAdapter.getCount();
    }

    private boolean isTopCard(View card) {
        return card == mCards.peek();
    }

    private boolean canAcceptChoice() {
        return Math.abs(mXDelta) > mMinAcceptDistance;
    }

    private void scaleAndTranslate(int cardIndex, View view) {
        LinearInterpolator interpolator = new LinearInterpolator();

        if (view == mBeingDragged){
            int sign = 1;
            if (mXDelta > 0){
                sign = -1;
            }
            float progress = Math.min(Math.abs(mXDelta) / ((float)mMinAcceptDistance*5), 1);
            float angleDegree = MAX_ANGLE_DEGREE * interpolator.getInterpolation(progress);

            view.setRotation(sign*angleDegree);

            return;
        }

        float zoomFactor = 0;

        if (mBeingDragged != null){
            float interpolation = 0;
            float distance = (float) Math.sqrt(mXDelta*mXDelta + mYDelta*mYDelta);
            float progress = Math.min(distance / mMinDragDistance, 1);
            interpolation = interpolator.getInterpolation(progress);
            interpolation = Math.min(interpolation, 1);
            zoomFactor = interpolation;
        }

        int position = STACK_SIZE - cardIndex;

        float step = 0.025f;

        Resources r = getContext().getResources();
        float translateStep = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, r.getDisplayMetrics());

        float scale = step * (position - zoomFactor);
        float translate = translateStep * (position - zoomFactor);
        view.setTranslationY(translate);
        view.setTranslationX(0);
        view.setRotation(0);
        view.setScaleX(1 - scale);
        view.setScaleY(1 - scale);

        return;
    }

    public CardStackListener getCardStackListener() {
        return mCardStackListener;
    }

    public void setCardStackListener(CardStackListener cardStackListener) {
        mCardStackListener = cardStackListener;
    }


    private static class MockListAdapter extends BaseAdapter {

        List<String> mItems;

        Context mContext;

        public MockListAdapter(Context context) {
            mContext = context;
            mItems = new ArrayList<String>();
            for (int i = 1; i < 15; i++) {
                mItems.add(i + "");
            }
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView view = new ImageView(mContext);
            view.setImageResource(R.drawable.content_card_x_00);

            Resources r = mContext.getResources();
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300, r.getDisplayMetrics());

            LayoutParams params = new LayoutParams(px, px);
            view.setLayoutParams(params);
            return view;
        }
    }

    private class MyOnTouchListener implements OnTouchListener {

        @Override
        public boolean onTouch(final View view, MotionEvent event) {

            VerticalViewPager pager = (VerticalViewPager) view.findViewById( R.id.verticalviewpager );
            //pager.setPagingEnabled( mBeingDragged == null );


            if (!isTopCard(view)){
                return false;
            }

            final int X = (int) event.getRawX();
            final int Y = (int) event.getRawY();

            final int action = event.getAction();
            switch (action & MotionEvent.ACTION_MASK) {

                case MotionEvent.ACTION_DOWN: {

                    mXStart = X;
                    mYStart = Y;

                    break;
                }
                case MotionEvent.ACTION_UP:

                    if (mBeingDragged == null) {
                        return false;
                    }

                    if (!canAcceptChoice()){
                        requestLayout();

                        AnimatorSet set = new AnimatorSet();

                        ObjectAnimator yTranslation = ObjectAnimator.ofFloat(mBeingDragged, "translationY", 0);
                        ObjectAnimator xTranslation = ObjectAnimator.ofFloat(mBeingDragged, "translationX", 0);
                        set.playTogether(
                            xTranslation
                        );

                        set.setDuration(100).start();
                        set.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {

                                View finalView = mBeingDragged;
                                mBeingDragged = null;
                                mXDelta = 0;
                                mYDelta = 0;
                                mXStart = 0;
                                mYStart = 0;
                                verticalMoreCount = 0;
                                verticalLessCount = 0;
                                mMovingVertically.clear();
                                requestLayout();

                                if (mCardStackListener != null){
                                    mCardStackListener.onCancelled(finalView);
                                }
                            }
                        });

                        ValueAnimator.AnimatorUpdateListener onUpdate = new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                mXDelta = (int) view.getTranslationX();
                                mYDelta = (int) view.getTranslationY();
                                requestLayout();
                            }
                        };

                        yTranslation.addUpdateListener(onUpdate);
                        xTranslation.addUpdateListener(onUpdate);

                        set.start();

                    }else{
                        final View last = mCards.poll();

                        View recycled = getRecycledOrNew();
                        if (recycled != null){
                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                            params.addRule(RelativeLayout.CENTER_IN_PARENT);

                            mCards.offer(recycled);
                            addView(recycled, 0, params);
                        }

                        int sign = mXDelta > 0 ? +1 : -1;
                        final boolean finalChoice = mXDelta > 0;

                        mBeingDragged= null;
                        mXDelta = 0;
                        mYDelta = 0;
                        mXStart = 0;
                        mYStart = 0;

                        ObjectAnimator animation = ObjectAnimator.ofFloat(last, "translationX", sign*1000)
                                .setDuration(300);
                        animation.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {

                                if (mCardStackListener != null){
                                    boolean choice = finalChoice;
                                    mCardStackListener.onChoiceMade(choice, last);
                                }

                                recycleView(last);

                                final ViewGroup parent = (ViewGroup) view.getParent();
                                if (null != parent) {
                                    parent.removeView(view);
                                    parent.addView(view, 0);
                                }


                                last.setScaleX(1);
                                last.setScaleY(1);
                                setTranslationY(0);
                                setTranslationX(0);
                                requestLayout();

                            }
                        });
                        animation.start();

                        verticalMoreCount = 0;
                        verticalLessCount = 0;
                        mMovingVertically.clear();
                    }

                    break;
                case MotionEvent.ACTION_MOVE:

                    int mDraggedY = Math.abs(Y - mYStart);
                    int mDraggedX = Math.abs(X - mXStart);

                    //Log.d(TAG, "Moving vertically more: " + (mDraggedY > mDraggedX) );
                    mMovingVertically.add((mDraggedY > mDraggedX));

                    for ( Boolean mv : mMovingVertically ){
                       if (mv)
                           verticalMoreCount++;
                        else
                           verticalLessCount++;
                    }
// Log.d(TAG, "mBeingDragged != null && verticalMoreCount > verticalLessCount): " + mBeingDragged + "  " + verticalMoreCount + " " + verticalLessCount);
                    if ( mBeingDragged != null && verticalMoreCount > verticalLessCount){

// Log.d(TAG, "mBeingDragged != null && verticalMoreCount > verticalLessCount)");
                        pager.setPagingEnabled(true);
                        // mBeingDragged = null;
                        return false;
                    } else {

                        pager.setPagingEnabled(false);
                    }

                    boolean choiceBoolean = getStackChoice();
                    float progress = getStackProgress();

                    view.setTranslationX(X - mXStart);
                    //view.setTranslationY(Y - mYStart);

                    mXDelta = X - mXStart;
                    mYDelta = Y - mYStart;

                    mBeingDragged = view;
                    requestLayout();

                    if (mCardStackListener != null){
                        mCardStackListener.onUpdateProgress(choiceBoolean, progress, mBeingDragged);
                    }

                    break;

            }
            return true;
        }

    }

    private void recycleView(View last) {
        ((ViewGroup)last.getParent()).removeView(last);
        mRecycledCards.offer(last);
    }

    private View getRecycledOrNew() {
        if (adapterHasMoreItems()){
            View view = mRecycledCards.poll();
            view = mAdapter.getView(mCurrentPosition++, view, null);

            return view;
        }else{
            return null;
        }
    }

    private boolean getStackChoice() {
        boolean choiceBoolean = false;
        if (mXDelta > 0){ // mXDelta > 0
            choiceBoolean = true;
        }
        return choiceBoolean;
    }

    private float getStackProgress() {
        LinearInterpolator interpolator = new LinearInterpolator();
        float progress = Math.min(Math.abs(mXDelta) / ((float)mMinAcceptDistance*5), 1);
        progress = interpolator.getInterpolation(progress);
        return progress;
    }

    public static class DummyAdapter extends FragmentStatePagerAdapter{

        public DummyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

//        @Override
//        public int getItemPosition(Object object) {
//            return POSITION_NONE;
//        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private static final String TAG = "PlaceholderFragment";

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_layout, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.textview);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }

    }

    public static class CardStackAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return null;
        }
    }

    private void initiateViewPager(View view){

        VerticalViewPager verticalViewPager = (VerticalViewPager) view.findViewById(R.id.verticalviewpager);

        verticalViewPager.setOffscreenPageLimit(5); // very important to set this, otherwise fragment will show the first time and disappear later
        verticalViewPager.setAdapter(new DummyAdapter(((FragmentActivity) getContext()).getSupportFragmentManager()));
        verticalViewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.pagemargin));
        //verticalViewPager.setPageMarginDrawable(new ColorDrawable(getResources().getColor(android.R.color.holo_green_dark)));
        verticalViewPager.setPageTransformer(true, new ViewPager.PageTransformer() {
            @Override
            public void transformPage(View view, float position) {

                int pageWidth = view.getWidth();
                int pageHeight = view.getHeight();

                if (position < -1) { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    view.setAlpha(0);

                } else if (position <= 1) { // [-1,1]
                    // Modify the default slide transition to shrink the page as well
                    float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                    float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                    float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                    if (position < 0) {
                        view.setTranslationY(vertMargin - horzMargin / 2);
                    } else {
                        view.setTranslationY(-vertMargin + horzMargin / 2);
                    }

                    // Scale the page down (between MIN_SCALE and 1)
                    //view.setScaleX(scaleFactor);
                    //view.setScaleY(scaleFactor);

                    // Fade the page relative to its size.
                    view.setAlpha(MIN_ALPHA +
                            (scaleFactor - MIN_SCALE) /
                                    (1 - MIN_SCALE) * (1 - MIN_ALPHA));

                } else { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    view.setAlpha(0);
                }
            }
        });

        CirclePageIndicator titleIndicator = (CirclePageIndicator) view.findViewById(R.id.verticalviewpager_indicator);
        titleIndicator.setOrientation(LinearLayout.VERTICAL);
        titleIndicator.setViewPager( verticalViewPager );

    }
}
