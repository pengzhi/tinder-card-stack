package com.lal.focusprototype.app;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.lal.focusprototype.app.views.CardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by diallo on 21/03/14.
 */
public class CardStackAdapter extends BaseAdapter {

    private static final String TAG = "CardStackAdapter";
    List<CardViewItem> mItems;

    Context context;

    public CardStackAdapter(Context context) {
        this.context = context;
        initAdapter();
    }

    void initAdapter() {

        mItems = new ArrayList<CardViewItem>();
        for(int i=1; i<= 10; i++){ //15
            // int index = i % 5 != 0 ? i % 5 : 1;
            mItems.add(new CardViewItem(i%5, i));
        }
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public CardViewItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        CardView personItemView;
        if (convertView == null) {
            personItemView = new CardView(context);
        } else {
            personItemView = (CardView) convertView;
        }

        personItemView.bind(getItem(position));

        return personItemView;
    }

    public void addItemToBottom(){

        mItems.add(new CardViewItem(mItems.size()%5,mItems.size()+1));
        notifyDataSetChanged();
    }

    public void removeItemFromTop(){

        mItems.remove(0);
    }
}
