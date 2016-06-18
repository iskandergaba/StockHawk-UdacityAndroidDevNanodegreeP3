package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

public class StocksHistoryService extends IntentService {

    public static final String BASE_URL = "http://query.yahooapis.com/v1/public/yql?q=";
    private StringBuilder mStoredSymbols = new StringBuilder();
    private Context mContext;
    private OkHttpClient client = new OkHttpClient();

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public StocksHistoryService(String name) {
        super(name);
    }

    public StocksHistoryService(){
        super(StocksHistoryService.class.getName());
    }

    String fetchData(String url) throws IOException{
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }

        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String startDate;
        if (cal.get(Calendar.MONTH) > 6) {
            startDate = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) - 6) +
                    "-" + cal.get(Calendar.DAY_OF_MONTH);
        } else {
            startDate = (cal.get(Calendar.YEAR) - 1) + "-" + (cal.get(Calendar.MONTH) + 7) +
                    "-" + (cal.get(Calendar.DAY_OF_MONTH) - 1);
        }

        String endDate = cal.get(Calendar.YEAR) + "-" + (cal.get(Calendar.MONTH) + 1)+
                "-" + cal.get(Calendar.DAY_OF_MONTH);

        StringBuilder urlStringBuilder = new StringBuilder();

        urlStringBuilder.append(BASE_URL);
        urlStringBuilder.append("select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20in%20(");

        if (intent.getStringExtra("tag").equals("init")) {
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("%20%22")
                            .append(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")))
                            .append("%22,");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                urlStringBuilder.append(mStoredSymbols);
            }
        }
        else if (intent.getStringExtra("tag").equals("add")) {
            String stockInput = intent.getStringExtra("symbol");
            urlStringBuilder.append("%22").append(stockInput).append("%22)");
        }

        urlStringBuilder.append("%20and%20startDate%20=%20%22").append(startDate).append("%22%20and%20endDate%20=%20%22")
                .append(endDate).append("%22&format=json&env=store://datatables.org/alltableswithkeys");

        String urlString;
        String getResponse = null;
        int result = GcmNetworkManager.RESULT_FAILURE;

        urlString = urlStringBuilder.toString();
        try{
            getResponse = fetchData(urlString);
            result = GcmNetworkManager.RESULT_SUCCESS;
            try {
                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                        Utils.quoteJsonToContentVals(getResponse, QuoteDatabase.QUOTES_HISTORY));
            }catch (RemoteException | OperationApplicationException e){
                Log.e("error", "Error applying batch insert", e);
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}