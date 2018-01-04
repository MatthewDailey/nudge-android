package com.reactiverobot.nudge;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

public class PackageSearchResultArrayAdapter extends PackageArrayAdapter {
    private static final String TAG = PackageSearchResultArrayAdapter.class.getName();

    private final Activity callingActivity;

    public PackageSearchResultArrayAdapter(@NonNull Activity context) {
        super(context);
        callingActivity = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);

        final PackageInfo packageInfo = getItem(position);

        view.findViewById(R.id.checkbox_block_package).setVisibility(View.INVISIBLE);

        view.setClickable(true);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs prefs = Prefs.from(getContext());

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
