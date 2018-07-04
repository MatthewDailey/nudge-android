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

import com.reactiverobot.nudge.info.PackageListManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo>
        implements PackageListManager.PackageListHandler, Prefs.CheckedSubscriber {
    private static final String TAG = PackageArrayAdapter.class.getName();

    private final Activity activity;
    private final CheckHandler checkHandler;
    private final PackageListManager packageListManager;

    public interface CheckHandler {
        void accept(PackageInfo packageInfo, boolean isChecked);
        boolean isChecked(PackageInfo packageInfo);
    }

    public static class Builder {
        private final PackageListManagerSupplier packageListManagerSupplier;
        private final Prefs prefs;

        private SearchView searchView = null;
        private Runnable onLoadPackagesComplete = null;

        public Builder(PackageListManagerSupplier packageListManagerSupplier, Prefs prefs) {
            this.packageListManagerSupplier = packageListManagerSupplier;
            this.prefs = prefs;
        }

        public Builder searchView(SearchView searchView) {
            this.searchView = searchView;
            return this;
        }

        public Builder onLoadPackagesComplete(Runnable onLoadPackagesComplete) {
            this.onLoadPackagesComplete = onLoadPackagesComplete;
            return this;
        }

        public PackageArrayAdapter attach(Activity activity, int listViewId, PackageType packageType) {
            PackageListManager packageListManager = packageListManagerSupplier.get(packageType);

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
                    }, packageListManager);

            ListView packageList = activity.findViewById(listViewId);
            packageList.setAdapter(packageAdapter);

            packageListManager.subscribe(packageAdapter);
            packageListManager.initialize(this.onLoadPackagesComplete);

            prefs.addSubscriber(packageListManager, packageType);
            prefs.addSubscriber(packageAdapter, packageType);

            if (this.searchView != null) {
                packageAdapter.withSearchView(searchView);
            }

            return packageAdapter;
        }
    }

    public static Builder builder(PackageListManagerSupplier packageListManagerSupplier, Prefs prefs) {
        return new Builder(packageListManagerSupplier, prefs);
    }

    public PackageArrayAdapter(@NonNull Activity context, CheckHandler checkHandler, PackageListManager packageListManager) {
        super(context, R.layout.list_item_package);

        this.activity = context;
        this.checkHandler = checkHandler;
        this.packageListManager = packageListManager;
    }

    public PackageArrayAdapter withSearchView(SearchView searchView) {
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

        return this;
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