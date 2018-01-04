package com.reactiverobot.nudge;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo> {
    public PackageArrayAdapter(@NonNull Context context) {
        super(context, R.layout.list_item_package);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(getContext());

        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.list_item_package, null);
        }

        TextView packageTextView = (TextView) convertView.findViewById(R.id.text_view_package_name);

        PackageInfo packageInfo = getItem(position);

        packageTextView.setText(packageInfo.packageName);

        return convertView;
    }
}
