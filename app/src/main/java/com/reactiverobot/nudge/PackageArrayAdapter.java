package com.reactiverobot.nudge;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.reactiverobot.nudge.prefs.PrefsImpl;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo> {
    private static final String TAG = PackageArrayAdapter.class.getName();

    private final RequestQueue requestQueue;

    public PackageArrayAdapter(@NonNull Context context) {
        super(context, R.layout.list_item_package);

        requestQueue = Volley.newRequestQueue(getContext());
    }

    private void updatePackageInfo(final PackageInfo packageInfo) {
        String url = "http://android-app-index.herokuapp.com/api/v1/get/" + packageInfo.packageName;

        JsonObjectRequest request = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        try {
                            packageInfo.name = response.getString("name");
                            packageInfo.iconUrl = response.getString("icon_url");

                            notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Failed to load package data.", error);
            }
        });

        requestQueue.add(request);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final PackageInfo packageInfo = getItem(position);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_item_package, null);

            CheckBox blockPackageCheckbox = (CheckBox) convertView.findViewById(
                    R.id.checkbox_block_package);
            blockPackageCheckbox.setOnCheckedChangeListener(
                    new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    PrefsImpl.from(getContext()).setPackageBlocked(packageInfo.packageName, isChecked);
                    packageInfo.blocked = isChecked;
                    notifyDataSetChanged();
                }
            });
        }

        if (packageInfo.name == null || packageInfo.iconUrl == null) {
            updatePackageInfo(packageInfo);
        }

        if (packageInfo.name != null) {
            ((TextView) convertView.findViewById(R.id.text_view_app_name))
                    .setText(packageInfo.name);
        }

        if (packageInfo.iconUrl != null) {
            Picasso.with(getContext())
                    .load(packageInfo.iconUrl)
                    .into((ImageView) convertView.findViewById(R.id.image_view_app_icon));
        }

        ((TextView) convertView.findViewById(R.id.text_view_package_name))
                .setText(packageInfo.packageName);

        ((CheckBox) convertView.findViewById(R.id.checkbox_block_package))
            .setChecked(packageInfo.blocked);

        return convertView;
    }
}
