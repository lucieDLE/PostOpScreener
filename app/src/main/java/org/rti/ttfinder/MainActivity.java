package org.rti.ttfinder;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.rti.ttfinder.data.preference.AppPreference;
import org.rti.ttfinder.data.preference.PrefKey;
import org.rti.ttfinder.databinding.ActivityMainBinding;
import org.rti.ttfinder.listeners.DialogActionListener;
import org.rti.ttfinder.ui.AdminLoginDialogFragment;
import org.rti.ttfinder.wrapper.TTProcessor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements DialogActionListener {

    private ActivityMainBinding binding;
    private boolean isChecked = true;
    private Context mContext;
    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mActivity = MainActivity.this;
        mContext = getApplicationContext();

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCv", "Error while init");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuItem = menu.findItem(R.id.checkable_menu);
        SwitchCompat mySwitch = menuItem.getActionView().findViewById(R.id.toggleAdmin);
        mySwitch.setChecked(AppPreference.getInstance(mActivity).getBoolean(PrefKey.IS_ADMIN, false));
        mySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d("Switch", String.valueOf(isChecked));
            if(!AppPreference.getInstance(mActivity).getBoolean(PrefKey.IS_ADMIN, false)){
                AdminLoginDialogFragment fragment = AdminLoginDialogFragment.newInstance();
                fragment.setListener(MainActivity.this);
                fragment.setCancelable(false);
                fragment.show(getSupportFragmentManager(),"AdminModeDialog");
            }
            else{
                mActivity.recreate();
                AppPreference.getInstance(mActivity).setBoolean(PrefKey.IS_ADMIN, false);
            }
        });
        return true;
    }

    @Override
    public void positiveBtn() {
        mActivity.recreate();
        AppPreference.getInstance(mActivity).setBoolean(PrefKey.IS_ADMIN, true);
    }

    @Override
    public void negativeBtn() {
        mActivity.recreate();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

}