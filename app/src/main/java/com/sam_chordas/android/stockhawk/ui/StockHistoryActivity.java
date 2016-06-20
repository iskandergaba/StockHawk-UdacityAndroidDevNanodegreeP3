package com.sam_chordas.android.stockhawk.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteHistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.view.LineChartView;

public class StockHistoryActivity extends AppCompatActivity
        implements OnNavigationListener {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * current dropdown position.
     */
    public static final String ARG_SYMBOL = "symbol";
    private static String STATE_PREF_NAVIGATION_ITEM = "pref_navigation_item";
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    private static final int CURSOR_LOADER_ID = 0;
    private static final int CURSOR_LOADER_ID_FOR_LINE_CHART = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_history);

        // Set up the action bar to show a dropdown list.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setElevation(0);
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);

            // Set up the dropdown list navigation in the action bar.
            actionBar.setListNavigationCallbacks(
                    // Specify a SpinnerAdapter to populate the dropdown list.
                    new ArrayAdapter<>(
                            actionBar.getThemedContext(),
                            android.R.layout.simple_list_item_1,
                            android.R.id.text1,
                            new String[]{
                                    getString(R.string.week),
                                    getString(R.string.two_weeks),
                                    getString(R.string.month)
                            }),
                    this);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            Integer navItem = prefs.getInt(STATE_PREF_NAVIGATION_ITEM, 0);
            actionBar.setSelectedNavigationItem(navItem);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        // Restore the previously serialized current dropdown position.
        if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
            getSupportActionBar().setSelectedNavigationItem(
                    savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Serialize the current dropdown position.
        outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                getSupportActionBar().getSelectedNavigationIndex());
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(STATE_PREF_NAVIGATION_ITEM, getSupportActionBar().getSelectedNavigationIndex());
        editor.apply();
    }

    @Override
    public boolean onNavigationItemSelected(int position, long id) {
        // When the given dropdown item is selected, show its contents in the
        // container view.
        getFragmentManager().beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position))
                .commit();
        return true;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public Intent getParentActivityIntent() {
        return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private String mSymbol;
        private LineChartView mChart;
        private TextView mSymbolTextView;
        private TextView mNameTextView;

        public PlaceholderFragment() {
        }

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
            View rootView = inflater.inflate(R.layout.fragment_stock_history, container, false);
            mSymbol = getActivity().getIntent().getStringExtra(ARG_SYMBOL);
            mSymbolTextView = (TextView)rootView.findViewById(R.id.history_stock_symbol);
            mNameTextView = (TextView)rootView.findViewById(R.id.history_stock_name);
            getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);
            getLoaderManager().initLoader(CURSOR_LOADER_ID_FOR_LINE_CHART, null, this);

            return rootView;
        }

        @Override
        public void onResume() {
            getLoaderManager().restartLoader(CURSOR_LOADER_ID_FOR_LINE_CHART, null, this);
            super.onResume();
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            if (id == CURSOR_LOADER_ID) {
                return new CursorLoader(getActivity().getApplicationContext(), QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.NAME},
                        QuoteColumns.SYMBOL + " = \"" + mSymbol + "\"",
                        null, null);


            }

            else if (id == CURSOR_LOADER_ID_FOR_LINE_CHART) {
                String sortOrder = QuoteHistoryColumns.DATE + " DESC";
                int position = getArguments().getInt(ARG_SECTION_NUMBER);
                if (position == 0) {
                    sortOrder = QuoteHistoryColumns.DATE + " DESC LIMIT 7";
                }
                else if (position == 1) {
                    sortOrder = QuoteHistoryColumns.DATE + " DESC LIMIT 15";
                }
                return new CursorLoader(getActivity().getApplicationContext(),
                        QuoteProvider.QuotesHistory.CONTENT_URI,
                        new String[]{QuoteHistoryColumns._ID, QuoteHistoryColumns.SYMBOL,
                                QuoteHistoryColumns.CLOSE_PRICE, QuoteHistoryColumns.DATE},
                        QuoteHistoryColumns.SYMBOL + " = \"" + mSymbol + "\"", null, sortOrder);
            }
            throw new IllegalArgumentException();
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (loader.getId() == CURSOR_LOADER_ID && data != null && data.moveToFirst()) {

                String symbol = data.getString(data.getColumnIndex(QuoteColumns.SYMBOL)).toUpperCase();
                String name = data.getString(data.getColumnIndex(QuoteColumns.NAME));
                if (name.equals("null")) {
                    name = getString(R.string.unknown_stock);
                }
                //noinspection EqualsBetweenInconvertibleTypes
                if (!mNameTextView.equals(name) || !mSymbolTextView.equals(symbol)) {
                    mSymbolTextView.setText(MessageFormat.format("NASDAQ: {0}", symbol));
                    mNameTextView.setText(name);
                }
            } else if (loader.getId() == CURSOR_LOADER_ID_FOR_LINE_CHART && data != null &&
                    data.moveToFirst()) {
                drawChart(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
        }

        private void drawChart(Cursor data) {
            mChart = (LineChartView)getActivity().findViewById(R.id.stock_change_chart);

            List<AxisValue> axisValuesX = new ArrayList<>();
            List<PointValue> pointValues = new ArrayList<>();

            int counter = -1;
            while (data.moveToNext()
                    && data.getColumnIndex(QuoteHistoryColumns.DATE) != -1)  {
                counter++;

            String date = data.getString(data.getColumnIndex(
                    QuoteHistoryColumns.DATE));
            float closePrice = data.getFloat(data.getColumnIndex(
                    QuoteHistoryColumns.CLOSE_PRICE));

                // We have to show chart in right order.
                int x = data.getCount() - 1 - counter;

                // Point for line chart (date, price).
                PointValue pointValue = new PointValue(x, closePrice);
                pointValue.setLabel(date);
                pointValues.add(pointValue);

                // Set labels for x-axis (we have to reduce its number to avoid overlapping text).
                if (counter != 0 && counter % (data.getCount() / 3) == 0) {
                    AxisValue axisValueX = new AxisValue(x);
                    axisValueX.setLabel(date);
                    axisValuesX.add(axisValueX);
                }

            }

            // Prepare data for chart
            Line line = new Line(pointValues).setColor(Color.WHITE).setCubic(false);
            List<Line> lines = new ArrayList<>();
            lines.add(line);
            LineChartData lineChartData = new LineChartData(lines);

            // Init x-axis
            Axis axisX = new Axis(axisValuesX);
            axisX.setHasLines(true);
            axisX.setMaxLabelChars(4);
            lineChartData.setAxisXBottom(axisX);

            // Init y-axis
            Axis axisY = new Axis();
            axisY.setAutoGenerated(true);
            axisY.setHasLines(true);
            axisY.setMaxLabelChars(4);
            lineChartData.setAxisYLeft(axisY);
            // Update chart with new data.
            mChart.setLineChartData(lineChartData);

            // Show chart
            mChart.setVisibility(View.VISIBLE);
            mChart.setInteractive(false);
        }
    }
}