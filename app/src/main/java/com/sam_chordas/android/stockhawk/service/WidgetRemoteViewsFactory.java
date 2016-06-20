package com.sam_chordas.android.stockhawk.service;


import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

public class WidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Cursor mCursor;
    private Context mContext;
    int mWidgetId;

    public WidgetRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }
    @Override
    public void onCreate() {
    }

    @Override
    public int getCount() {
        if (mCursor != null) {
            return mCursor.getCount();
        }
        return 0;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.list_item_quote);
        if (mCursor.moveToPosition(position)) {
            rv.setTextViewText(R.id.stock_symbol,
                    mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL)));
            rv.setTextViewText(R.id.bid_price,
                    mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE)));
            rv.setTextViewText(R.id.change,
                    mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE)));
        }
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        if (mCursor.moveToPosition(position))
            return mCursor.getLong(mCursor.getColumnIndex(QuoteColumns._ID));
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onDataSetChanged() {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }
}