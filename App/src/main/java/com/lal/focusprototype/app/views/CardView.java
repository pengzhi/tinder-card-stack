package com.lal.focusprototype.app.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lal.focusprototype.app.CardViewItem;
import com.lal.focusprototype.app.R;

/**
 * Created by diallo on 21/03/14.
 */
public class CardView extends RelativeLayout implements CardStackView.CardStackListener {

    ImageView picture;

    TextView id;

    TextView ok;

    TextView no;

    private CardViewItem mCardViewItem;

    public CardView(Context context) {
        super(context);
        LayoutInflater.from(context).inflate(R.layout.feed_item, this, true);
        picture = (ImageView) findViewById(R.id.picture);
        id = (TextView) findViewById(R.id.id_textView);
        ok = (TextView) findViewById(R.id.ok);
        no = (TextView) findViewById(R.id.no);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

    }

    public void bind(CardViewItem item) {
        mCardViewItem = item;

        return;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mCardViewItem != null) {
            int resource = getResources().getIdentifier(
                    "content_card_x_0" + mCardViewItem.getId(),
                    "drawable", getContext().getPackageName());

            // loadPicture(resource);

            id.setText(mCardViewItem.toString());
        }

    }

    public CardViewItem getFeedItem() {
        return mCardViewItem;
    }

    void loadPicture(int id) {
        Drawable drawable = getResources().getDrawable(id);

        setPicture(drawable);
    }

    void setPicture(Drawable drawable) {
        picture.setScaleType(ImageView.ScaleType.FIT_XY);
        picture.setImageDrawable(drawable);
    }

    @Override
    public void onUpdateProgress(boolean positif, float percent, View view) {
        if (positif) {
            ok.setAlpha(percent);
        } else {
            no.setAlpha(percent);
        }
    }

    @Override
    public void onCancelled(View beingDragged) {
        ok.setAlpha(0);
        no.setAlpha(0);
    }

    @Override
    public void onChoiceMade(boolean choice, View beingDragged) {
        ok.setAlpha(0);
        no.setAlpha(0);
    }
}
