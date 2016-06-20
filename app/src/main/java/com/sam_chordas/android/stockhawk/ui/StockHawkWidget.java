package com.sam_chordas.android.stockhawk.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.service.WidgetRemoteViewsService;

/**
 * Implementation of App Widget functionality.
 */
public class StockHawkWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        for (int i : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, i);
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                      int appWidgetId) {
        RemoteViews rv = new RemoteViews(context.getPackageName(),
                R.layout.stock_hawk_widget);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, new Intent(context, MyStocksActivity.class), PendingIntent.FLAG_CANCEL_CURRENT);
        rv.setOnClickPendingIntent(R.id.logo, pendingIntent);
        setList(rv, context, appWidgetId);
        rv.setEmptyView(R.id.widget_list, R.id.widget_empty);
        appWidgetManager.updateAppWidget(appWidgetId, rv);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId,
                R.id.widget_list);
    }

    static void setList(RemoteViews rv, Context context, int appWidgetId) {
        Intent adapter = new Intent(context, WidgetRemoteViewsService.class);
        adapter.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        rv.setRemoteAdapter(R.id.widget_list, adapter);
    }
}