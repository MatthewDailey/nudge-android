package com.reactiverobot.nudge;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.reactiverobot.nudge.info.PackageListManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;

import java.util.List;

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo>
        implements PackageListManager.PackageListHandler {
    private static final String TAG = PackageArrayAdapter.class.getName();

    private final Activity activity;
    @NonNull private final PackageType packageType;
    private final CheckHandler checkHandler;
    private final PackageListManager packageListManager;

    @Nullable private final PackageInfoHandler clickHandler;
    @Nullable private final PackageInfoHandler longPressHandler;
    @Nullable private final PackageInfoHandler displayBrieflyHandler;


    public interface PackageInfoHandler {
        void handle(PackageType addapterPackageType, PackageInfo packageInfo);
    }

    public interface CheckHandler {
        void accept(PackageInfo packageInfo, boolean isChecked);
        boolean isChecked(PackageInfo packageInfo);
    }

    public static class Builder {
        private final PackageListManagerSupplier packageListManagerSupplier;
        private final Prefs prefs;

        private SearchView searchView = null;
        private Runnable onLoadPackagesComplete = null;
        private boolean shouldIncludeCheckbox = false;

        private PackageInfoHandler longPressHandler = null;
        private PackageInfoHandler clickHandler = null;
        private PackageInfoHandler displayBrieflyHandler = null;

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

        public Builder withCheckbox() {
            this.shouldIncludeCheckbox = true;
            return this;
        }

        public Builder onLongPress(PackageInfoHandler longPressHandler) {
            this.longPressHandler = longPressHandler;
            return this;
        }

        public Builder onClick(PackageInfoHandler clickHandler) {
            this.clickHandler = clickHandler;
            return this;
        }

        public PackageArrayAdapter attach(Activity activity, PackageType packageType) {
            PackageListManager packageListManager = packageListManagerSupplier.get(packageType);

            CheckHandler checkHandler = new CheckHandler() {
                @Override
                public void accept(PackageInfo packageInfo, boolean isChecked) {
                    prefs.setPackageSelected(packageType, packageInfo.packageName, isChecked);
                }

                @Override
                public boolean isChecked(PackageInfo packageInfo) {
                    return packageInfo.isSelected(packageType);
                }
            };
            PackageArrayAdapter packageAdapter = new PackageArrayAdapter(
                    activity,
                    packageType,
                    shouldIncludeCheckbox ? checkHandler : null,
                    displayBrieflyHandler,
                    packageListManager,
                    clickHandler,
                    longPressHandler);

            packageListManager.subscribe(packageAdapter);
            packageListManager.initialize(this.onLoadPackagesComplete);

            prefs.addSubscriber(packageListManager, packageType);

            if (this.searchView != null) {
                packageAdapter.withSearchView(searchView);
            }

            return packageAdapter;
        }
    }

    public static Builder builder(PackageListManagerSupplier packageListManagerSupplier, Prefs prefs) {
        return new Builder(packageListManagerSupplier, prefs);
    }

    public PackageArrayAdapter(@NonNull Activity context,
                               @NonNull PackageType packageType,
                               @Nullable CheckHandler checkHandler,
                               @Nullable PackageInfoHandler displayBrieflyHandler,
                               @NonNull PackageListManager packageListManager,
                               @Nullable PackageInfoHandler clickHandler,
                               @Nullable PackageInfoHandler longPressHandler) {
        super(context, R.layout.list_item_package);

        this.activity = context;
        this.packageType = packageType;
        this.checkHandler = checkHandler;
        this.displayBrieflyHandler = displayBrieflyHandler;
        this.packageListManager = packageListManager;
        this.clickHandler = clickHandler;
        this.longPressHandler = longPressHandler;
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
        if (this.checkHandler != null) {
            blockPackageCheckbox.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> this.checkHandler.accept(packageInfo, isChecked));
            ((CheckBox) convertView.findViewById(R.id.checkbox_block_package))
                    .setChecked(checkHandler.isChecked(packageInfo));
        } else {
            blockPackageCheckbox.setVisibility(View.GONE);
        }

        ImageButton clockButton = convertView.findViewById(R.id.button_briefly_show);
        if (this.displayBrieflyHandler != null) {
            clockButton.setOnClickListener((view) -> this.displayBrieflyHandler.handle(packageType, packageInfo));
        } else {
            clockButton.setVisibility(View.GONE);
        }

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

        if (this.clickHandler != null) {
            convertView.setOnClickListener(view -> {
                Log.d(TAG, "CLICKED");
                this.clickHandler.handle(packageType, packageInfo);
            });
        }

        if (this.longPressHandler != null) {
            convertView.setOnLongClickListener(view -> {
                this.longPressHandler.handle(packageType, packageInfo);
                return true;
            });
        }

        return convertView;
    }

    @Override
    public void accept(List<PackageInfo> packageInfos) {
        this.activity.runOnUiThread(() -> {
            clear();
            addAll(packageInfos);
        });
    }
}