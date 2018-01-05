package com.reactiverobot.nudge;

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.reactiverobot.nudge.di.test.TestInterface;
import com.reactiverobot.nudge.prefs.Prefs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = SearchActivity.class.getName();

    @Inject
    TestInterface testInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Select a Bad Habit app");
        toolbar.setTitleTextColor(getColor(R.color.black));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Drawable upArrow = getDrawable(R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(getColor(R.color.black), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        testInterface.coolMethod();

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "Received search query '" + query + "'");

            final RequestQueue requestQueue = Volley.newRequestQueue(this);

            ListView searchResultsListView = (ListView)
                    findViewById(R.id.list_view_search_results);

            final PackageArrayAdapter packageArrayAdapter = new PackageSearchResultArrayAdapter(this);
            searchResultsListView.setAdapter(packageArrayAdapter);

            if (query != null && !query.toString().isEmpty()) {

                ((TextView) findViewById(R.id.text_view_search_query))
                        .setText("Loading search results for '" + query + "'...");

                // TODO: Try using algolia android api for this instead.
                String url = "https://android-app-index.herokuapp.com/api/v1/search?q=" + query;

                StringRequest request = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                ((TextView) findViewById(R.id.text_view_search_query))
                                        .setText("Showing search results for '" + query + "'");
                                findViewById(R.id.progress_bar_search_results).setVisibility(View.GONE);

                                try {
                                    JSONArray jsonarray = new JSONArray(response);
                                    for (int i = 0; i < jsonarray.length(); i++) {
                                        JSONObject jsonobject = jsonarray.getJSONObject(i);
                                        packageArrayAdapter.add(new PackageInfo(
                                                jsonobject.getString("name"),
                                                jsonobject.getString("icon_url"),
                                                jsonobject.getString("package"),
                                                true));
                                    }

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }


                                Log.d(TAG, "Search results: " + response);
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.e(TAG, "Failed to load package data.", error);
                            }
                        });

                requestQueue.add(request);
            } else {
                Log.d(TAG, "Empty query.");
            }
        }

    }

}
