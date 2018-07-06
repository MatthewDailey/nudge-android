package com.reactiverobot.nudge;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.reactiverobot.nudge.info.PackageListManager;
import com.reactiverobot.nudge.info.PackageListManagerSupplier;
import com.reactiverobot.nudge.info.PackageType;
import com.reactiverobot.nudge.prefs.Prefs;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class PackageArrayAdapter extends ArrayAdapter<PackageInfo>
        implements PackageListManager.PackageListHandler {
    private static final String TAG = PackageArrayAdapter.class.getName();

    private final Activity activity;
    private final CheckHandler checkHandler;
    private final PackageListManager packageListManager;

    @Nullable private final PackageInfoHandler clickHandler;
    @Nullable private final PackageInfoHandler longPressHandler;

    public interface PackageInfoHandler {
        void handle(PackageInfo packageInfo);
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
                    shouldIncludeCheckbox ? checkHandler : null,
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
                               @Nullable CheckHandler checkHandler,
                               @NonNull PackageListManager packageListManager,
                               @Nullable PackageInfoHandler clickHandler,
                               @Nullable PackageInfoHandler longPressHandler) {
        super(context, R.layout.list_item_package);

        this.activity = context;
        this.checkHandler = checkHandler;
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

        if (this.clickHandler != null) {
            convertView.setOnClickListener(view -> {
                Log.d(TAG, "CLICKED");
                this.clickHandler.handle(packageInfo);
            });
        }

        if (this.longPressHandler != null) {
            convertView.setOnLongClickListener(view -> {
                this.longPressHandler.handle(packageInfo);
                return true;
            });
        }

        return convertView;
    }

    @Override
    public void accept(List<PackageInfo> packageInfos) {
        Log.d(TAG, "accipting packages");
        this.activity.runOnUiThread(() -> {
            clear();
            addAll(packageInfos);
        });
    }
}