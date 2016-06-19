package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteDatabase;
import com.sam_chordas.android.stockhawk.data.QuoteHistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList quoteJsonToContentVals(String JSON, String param){
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject;
        JSONArray resultsArray;
        try{
            jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0){
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1){
                    jsonObject = jsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    batchOperations.add(buildBatchOperation(jsonObject, param));
                } else {
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsArray != null && resultsArray.length() != 0){
                        for (int i = 0; i < resultsArray.length(); i++){
                            jsonObject = resultsArray.getJSONObject(i);
                            batchOperations.add(buildBatchOperation(jsonObject, param));
                        }
                    }
                }
            }
        } catch (JSONException e){
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice){
        if (!bidPrice.equals("null")) {
            bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        } else {
            bidPrice = "Not found";
        }
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange){
        if (!change.equals("null")) {
            String weight = change.substring(0, 1);
            String ampersand = "";
            if (isPercentChange) {
                ampersand = change.substring(change.length() - 1, change.length());
                change = change.substring(0, change.length() - 1);
            }
            change = change.substring(1, change.length());
            double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
            change = String.format("%.2f", round);
            StringBuilder changeBuffer = new StringBuilder(change);
            changeBuffer.insert(0, weight);
            changeBuffer.append(ampersand);
            change = changeBuffer.toString();
        } else {
            change = "--.--";
        }
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, String param){
        ContentProviderOperation.Builder builder = null;
        if (param.equals(QuoteDatabase.QUOTES)) {
            builder = ContentProviderOperation.newInsert(
                    QuoteProvider.Quotes.CONTENT_URI);
            try {
                String change = jsonObject.getString("Change");
                builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
                builder.withValue(QuoteColumns.NAME, jsonObject.getString("Name"));
                builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
                builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                        jsonObject.getString("ChangeinPercent"), true));
                builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
                builder.withValue(QuoteColumns.ISCURRENT, 1);
                if (change.charAt(0) == '-'){
                    builder.withValue(QuoteColumns.ISUP, 0);
                }else {
                    builder.withValue(QuoteColumns.ISUP, 1);
                }

            } catch (JSONException e){
                e.printStackTrace();
            }
        }

        else if(param.equals(QuoteDatabase.QUOTES_HISTORY)) {
            builder = ContentProviderOperation.newInsert(
                    QuoteProvider.QuotesHistory.CONTENT_URI);
            try {
                builder.withValue(QuoteHistoryColumns.SYMBOL, jsonObject.getString("Symbol"));
                builder.withValue(QuoteHistoryColumns.DATE, jsonObject.getString("Date"));
                builder.withValue(QuoteHistoryColumns.CLOSE_PRICE, Double.parseDouble(jsonObject.getString("Close")));

            } catch (JSONException e){
                e.printStackTrace();
            }
        }
        return builder.build();
    }

    public static int errorMsgId() {
        if (MyStocksActivity.isConnected) {
            return R.string.empty_stock_list;
        }
        return R.string.network_unavailable;
    }
}