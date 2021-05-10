package com.kegel.booker;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.preference.PreferenceManager;

import com.garmin.android.connectiq.ConnectIQ;
import com.garmin.android.connectiq.IQApp;
import com.garmin.android.connectiq.IQDevice;
import com.garmin.android.connectiq.exception.InvalidStateException;
import com.kegel.booker.book.Helpers;
import com.kegel.booker.media.OnBookCompleteListener;
import com.kegel.booker.ui.UIHelpers;
import com.kegel.booker.watch.ConfigTransmitter;
import com.kegel.booker.watch.WatchHelpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookerActivity extends AppCompatActivity {

    private Menu menu;
    private PlaybackFragment playbackFragment;
    private ConfigTransmitter configTransmitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
        });

        Helpers.setBookCompleteListener(this, new OnBookCompleteListener() {
            @Override
            public void onComplete() {
                if (!UIHelpers.isCurrentFragment(BookerActivity.this, LibraryFragment.class)) {
                    Navigation.findNavController(BookerActivity.this, R.id.nav_host_fragment)
                            .navigate(R.id.action_FirstFragment_to_LibraryFragment, null, new NavOptions.Builder().setLaunchSingleTop(true).build());
                    getSupportActionBar().setDisplayShowHomeEnabled(false);
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                    enableMenu(false);
                }
            }
        });
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        configTransmitter = new ConfigTransmitter(getApplicationContext());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (getSupportFragmentManager().getPrimaryNavigationFragment().getChildFragmentManager().getFragments().size() > 0) {
            if (getSupportFragmentManager().getPrimaryNavigationFragment().getChildFragmentManager().getFragments().get(0) instanceof PlaybackFragment) {
                playbackFragment = (PlaybackFragment) getSupportFragmentManager().getPrimaryNavigationFragment().getChildFragmentManager().getFragments().get(0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        this.menu = menu;
        return true;
    }

    public void enableMenu(boolean enable) {
        if (menu != null) {
            menu.setGroupVisible(R.id.action_menu_all, enable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (!UIHelpers.isCurrentFragment(this, SettingsFragment.class)) {
                Navigation.findNavController(this, R.id.nav_host_fragment)
                        .navigate(R.id.action_FirstFragment_to_settingsFragment, null, new NavOptions.Builder().setLaunchSingleTop(true).build());
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                enableMenu(false);
                return true;
            }
            return false;
        }

        else if (id == R.id.action_sync) {
            if (!UIHelpers.isCurrentFragment(this, SyncFragment.class)) {
                Navigation.findNavController(this, R.id.nav_host_fragment)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment, null, new NavOptions.Builder().setLaunchSingleTop(true).build());
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                enableMenu(false);
                return true;
            }
            return false;
        }

        else if (id == R.id.action_library) {
            if (!UIHelpers.isCurrentFragment(this, LibraryFragment.class)) {
                Navigation.findNavController(this, R.id.nav_host_fragment)
                        .navigate(R.id.action_FirstFragment_to_LibraryFragment, null, new NavOptions.Builder().setLaunchSingleTop(true).build());
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                enableMenu(false);
                return true;
            }
            return false;
        }



        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (UIHelpers.isCurrentFragment(this, LibraryFragment.class)) {
            if (playbackFragment.isBookComplete()) {
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean onSupportNavigateUp(){
        Navigation.findNavController(this, R.id.nav_host_fragment).popBackStack();
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        enableMenu(true);

                //.navigate(R.id.action_FirstFragment_to_settingsFragment, null, new NavOptions.Builder().setLaunchSingleTop(true).build());
        return true;
    }

}