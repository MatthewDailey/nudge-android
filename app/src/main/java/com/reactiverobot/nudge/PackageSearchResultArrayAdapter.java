package com.reactiverobot.nudge;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.reactiverobot.nudge.info.PackageInfoManager;
import com.reactiverobot.nudge.info.PackageInfoManagerImpl;
import com.reactiverobot.nudge.prefs.PrefsImpl;

public class PackageSearchResultArrayAdapter extends PackageArrayAdapter {
    private static final String TAG = PackageSearchResultArrayAdapter.class.getName();

    private final Activity callingActivity;

    public PackageSearchResultArrayAdapter(@NonNull Activity context) {
        super(context, PackageInfoManagerImpl.builder(context.getPackageManager()).build(context));
        callingActivity = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        final PackageInfo packageInfo = this.packageInfoManager.get(getItem(position));

        view.findViewById(R.id.checkbox_block_package).setVisibility(View.INVISIBLE);

        view.setClickable(true);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PrefsImpl prefs = PrefsImpl.from(getContext());

                prefs.setPackageBlocked(packageInfo.packageName, true);
                prefs.setPackagePinned(packageInfo.packageName, true);

                Toast toast = Toast.makeText(getContext(),
                        packageInfo.name + " has been marked as a bad habit. You'll be nudged away from it!",
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();

                callingActivity.finish();
            }
        });

        return view;
    }
}
