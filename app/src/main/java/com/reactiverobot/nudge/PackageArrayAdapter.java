package com.reactiverobot.nudge;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo> {
    private static final String TAG = PackageArrayAdapter.class.getName();

    private final RequestQueue requestQueue;

    private Optional<String> filter = Optional.empty();
    private List<PackageInfo> filteredOut = new ArrayList<>();

    public PackageArrayAdapter(@NonNull Context context) {
        super(context, R.layout.list_item_package);

        requestQueue = Volley.newRequestQueue(getContext());
    }

    private void updatePackageInfo(final PackageInfo packageInfo) {
        try {
            PackageManager packageManager = getContext().getPackageManager();

            Drawable appIcon = packageManager.getApplicationIcon(packageInfo.packageName);
            String appName = packageManager.getApplicationInfo(packageInfo.packageName, 0)
                    .loadLabel(packageManager).toString();

            packageInfo.name = appName;
            packageInfo.iconDrawable = appIcon;
        } catch (PackageManager.NameNotFoundException e) {
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
    }

    public void setPackageInfos(Collection<PackageInfo> packageInfos) {
        clear();
        addAll(packageInfos);
        sort();
    }

    private void sort() {
        sort(new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo o1, PackageInfo o2) {
                if (o1.blocked && !o2.blocked) {
                    return -1;
                } else if (o2.blocked && !o1.blocked) {
                    return 1;
                }

                if (o1.name == null || o2.name == null) {
                    return o1.packageName.compareTo(o2.packageName);
                }

                return o1.name.compareTo(o2.name);
            }
        });
    }

    public void setFilter(final Optional<String> filter) {
        int packageCount = getCount();

        final List<PackageInfo> filteredOut = new ArrayList<>();
        final List<PackageInfo> filteredIn = new ArrayList<>();

        List<PackageInfo> allPackages = new ArrayList<>();
        allPackages.addAll(this.filteredOut);
        for (int i = 0; i < packageCount; i++) {
            allPackages.add(getItem(i));
        }

        if (filter.isPresent()) {
            allPackages.forEach(new Consumer<PackageInfo>() {
                @Override
                public void accept(PackageInfo packageInfo) {
                    if (packageInfo.name == null
                        || !packageInfo.name.toLowerCase().contains(filter.get().toLowerCase())) {
                        filteredOut.add(packageInfo);
                    } else {
                        filteredIn.add(packageInfo);
                    }
                }
            });

            this.filteredOut = filteredOut;
            setPackageInfos(filteredIn);
        } else {
            this.filteredOut = new ArrayList<>();
            setPackageInfos(allPackages);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final PackageInfo packageInfo = getItem(position);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_item_package, null);
        }

        CheckBox blockPackageCheckbox = (CheckBox) convertView.findViewById(
                R.id.checkbox_block_package);
        blockPackageCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        PrefsImpl.from(getContext()).setPackageBlocked(packageInfo.packageName, isChecked);
                        packageInfo.blocked = isChecked;
                        sort();
                    }
                });

        if (packageInfo.name == null
                || (packageInfo.iconUrl == null && packageInfo.iconDrawable == null)) {
            updatePackageInfo(packageInfo);
        }

        if (packageInfo.name != null) {
            ((TextView) convertView.findViewById(R.id.text_view_app_name))
                    .setText(packageInfo.name);
        }

        if (packageInfo.iconDrawable != null) {
            ((ImageView) convertView.findViewById(R.id.image_view_app_icon))
                    .setImageDrawable(packageInfo.iconDrawable);
        } else if (packageInfo.iconUrl != null) {
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
