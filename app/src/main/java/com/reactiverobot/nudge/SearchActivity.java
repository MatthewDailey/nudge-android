package com.reactiverobot.nudge;

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class SearchActivity extends AppCompatActivity {

    private static final String TAG = SearchActivity.class.getName();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Pick an app");
        toolbar.setTitleTextColor(getColor(R.color.black));
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Drawable upArrow = getDrawable(R.drawable.abc_ic_ab_back_material);
        upArrow.setColorFilter(getColor(R.color.black), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            Log.d(TAG, "Received search query '" + query + "'");

            final RequestQueue requestQueue = Volley.newRequestQueue(this);

            if (query != null && !query.toString().isEmpty()) {
                String url = "https://android-app-index.herokuapp.com/api/v1/search?q=" + query;

                StringRequest request = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
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
