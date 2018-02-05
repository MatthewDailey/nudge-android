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
import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManager;
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

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo>
        implements PackageListManager.PackageListHandler {
    private static final String TAG = PackageArrayAdapter.class.getName();

    public PackageArrayAdapter(@NonNull Context context) {
        super(context, R.layout.list_item_package);
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
                (buttonView, isChecked) -> {
                    PrefsImpl.from(getContext()).setPackageBlocked(packageInfo.packageName, isChecked);
                    packageInfo.blocked = isChecked;
                });

        if (packageInfo.iconDrawable != null) {
            ((ImageView) convertView.findViewById(R.id.image_view_app_icon))
                    .setImageDrawable(packageInfo.iconDrawable);
        } else if (packageInfo.iconUrl != null) {
            Picasso.with(getContext())
                    .load(packageInfo.iconUrl)
                    .into((ImageView) convertView.findViewById(R.id.image_view_app_icon));
        } else {
            ((ImageView) convertView.findViewById(R.id.image_view_app_icon))
                    .setImageDrawable(null);
        }

        ((TextView) convertView.findViewById(R.id.text_view_app_name))
                .setText(packageInfo.name);

        ((TextView) convertView.findViewById(R.id.text_view_package_name))
                .setText(packageInfo.packageName);

        ((CheckBox) convertView.findViewById(R.id.checkbox_block_package))
            .setChecked(packageInfo.blocked);

        return convertView;
    }

    @Override
    public void accept(List<PackageInfo> packageInfos) {
        addAll(packageInfos);
        notifyDataSetChanged();
    }

    @Override
    public void update() {
        notifyDataSetChanged();
    }
}
