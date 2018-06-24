package com.reactiverobot.nudge;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageListManager;
import com.reactiverobot.nudge.info.PackageListManagerImpl;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.List;

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo>
        implements PackageListManager.PackageListHandler, Prefs.CheckedSubscriber {
    private static final String TAG = PackageArrayAdapter.class.getName();

    private final Activity activity;
    private final CheckHandler checkHandler;

    public interface CheckHandler {
        void accept(PackageInfo packageInfo, boolean isChecked);
        boolean isChecked(PackageInfo packageInfo);
    }

    public static void attach(Activity activity,
                              PackageType packageType,
                              PackageInfoManager packageInfoManager,
                              Prefs prefs,
                              int listViewId,
                              int searchViewId) {
        PackageArrayAdapter packageAdapter = new PackageArrayAdapter(
                activity,
                new PackageArrayAdapter.CheckHandler() {
                    @Override
                    public void accept(PackageInfo packageInfo, boolean isChecked) {
                        packageInfo.setSelected(packageType, isChecked);
                        prefs.setPackageSelected(packageType, packageInfo.packageName, isChecked);
                    }

                    @Override
                    public boolean isChecked(PackageInfo packageInfo) {
                        return packageInfo.isSelected(packageType);
                    }
                });

        ListView badHabitsList = activity.findViewById(listViewId);
        badHabitsList.setAdapter(packageAdapter);

        PackageListManagerImpl packageListManager = new PackageListManagerImpl(
                activity.getPackageManager(),
                packageInfoManager,
                () -> prefs.getPinnedPackages(packageType));
        packageListManager.subscribe(packageAdapter);
        packageListManager.initialize();

        prefs.addSubscriber(packageListManager, packageType);
        prefs.addSubscriber(packageAdapter, packageType);

        SearchView searchView = activity.findViewById(searchViewId);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newQuery) {
                packageListManager.setFilter(newQuery);
                return true;
            }
        });
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
        } else {
            ((ImageView) convertView.findViewById(R.id.image_view_app_icon))
                    .setImageDrawable(null);
        }

        ((TextView) convertView.findViewById(R.id.text_view_app_name))
                .setText(packageInfo.name);

        ((TextView) convertView.findViewById(R.id.text_view_package_name))
                .setText(packageInfo.packageName);

        ((CheckBox) convertView.findViewById(R.id.checkbox_block_package))
            .setChecked(checkHandler.isChecked(packageInfo));

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
    public void onCheckedUpdate() {
        this.activity.runOnUiThread(() -> notifyDataSetChanged());
    }
}