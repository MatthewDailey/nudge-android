package com.reactiverobot.nudge;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.reactiverobot.nudge.info.PackageListManager;
import com.reactiverobot.nudge.prefs.Prefs;
import com.reactiverobot.nudge.prefs.PrefsImpl;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo>
        implements PackageListManager.PackageListHandler, Prefs.CheckedSubscriber {
    private static final String TAG = PackageArrayAdapter.class.getName();

    private final Activity activity;
    private final CheckHandler checkHandler;

    public interface CheckHandler {
        void accept(PackageInfo packageInfo, boolean isChecked);
    }

    public PackageArrayAdapter(@NonNull Activity context, CheckHandler checkHandler) {
        super(context, R.layout.list_item_package);

        this.activity = context;
        this.checkHandler = checkHandler;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).type == PackageInfo.Type.HEADING ? 1 : 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final PackageInfo packageInfo = getItem(position);

        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        if (convertView == null) {
            if (PackageInfo.Type.PACKAGE.equals(packageInfo.type)) {
                convertView = layoutInflater.inflate(R.layout.list_item_package, null);
            } else {
                convertView = layoutInflater.inflate(R.layout.list_item_heading, null);
            }
        }

        if (PackageInfo.Type.HEADING.equals(packageInfo.type)) {
            ((TextView) convertView.findViewById(R.id.text_view_package_header)).setText(packageInfo.packageName);
            return convertView;
        }

        CheckBox blockPackageCheckbox = convertView.findViewById(
                R.id.checkbox_block_package);
        blockPackageCheckbox.setOnCheckedChangeListener(
                (buttonView, isChecked) -> this.checkHandler.accept(packageInfo, isChecked));

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
            .setChecked(packageInfo.badHabit);

        return convertView;
    }

    @Override
    public void accept(List<PackageInfo> packageInfos) {
        this.activity.runOnUiThread(() -> {
            clear();
            addAll(packageInfos);
        });
    }

    @Override
    public void update() {
        this.activity.runOnUiThread(() -> notifyDataSetChanged());
    }

    @Override
    public void onCheckedUpdate() {
        this.activity.runOnUiThread(() -> notifyDataSetChanged());
    }
}