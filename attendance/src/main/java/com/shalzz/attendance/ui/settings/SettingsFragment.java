/*
 * Copyright (c) 2013-2016 Shaleen Jain <shaleen.jain95@gmail.com>
 *
 * This file is part of UPES Academics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shalzz.attendance.ui.settings;

import android.app.NotificationManager;
import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

import com.bugsnag.android.Bugsnag;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.MyApplication;
import com.shalzz.attendance.R;
import com.shalzz.attendance.ui.main.MainActivity;
import com.shalzz.attendance.wrapper.MySyncManager;

import javax.inject.Inject;
import javax.inject.Named;

import timber.log.Timber;

public class SettingsFragment extends PreferenceFragmentCompat implements OnSharedPreferenceChangeListener{

    private Context mContext;
    private String key_sub_limit;
    private String key_sync_interval;
    private String key_sync_day_night;

    @Inject
    @Named("app")
    Tracker mTracker;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        mContext = getActivity();
        MyApplication.get(mContext).getComponent().inject(this);
	    Bugsnag.setContext("Settings");

        addPreferencesFromResource(R.xml.preferences);

        key_sync_day_night = getString(R.string.pref_key_day_night);
        ListPreference dayNightListPref = (ListPreference) findPreference(key_sync_day_night);
        dayNightListPref.setSummary(dayNightListPref.getEntry());

        key_sub_limit = getString(R.string.pref_key_sub_limit);
        ListPreference listPref = (ListPreference) findPreference(key_sub_limit);
        listPref.setSummary(listPref.getEntry());

        key_sync_interval = getString(R.string.pref_key_sync_interval);
        ListPreference synclistPref = (ListPreference) findPreference(key_sync_interval);
        synclistPref.setSummary(synclistPref.getEntry());
    }

    @Override
    public void onStart() {
        super.onStart();

        mTracker.setScreenName(getClass().getSimpleName());
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        requestBackup();

        if(key.equals(key_sync_day_night)) {
            ListPreference connectionPref = (ListPreference) findPreference(key);
            connectionPref.setSummary(connectionPref.getEntry());
            //noinspection WrongConstant
            AppCompatDelegate.setDefaultNightMode(Integer.parseInt(sharedPreferences.
                    getString(key,"-1")));
        }
        else if(key.equals(key_sub_limit)) {
            ListPreference connectionPref = (ListPreference) findPreference(key);
            connectionPref.setSummary(connectionPref.getEntry());
        }
        else if (key.equals(getString(R.string.pref_key_sync))) {
            DatabaseHandler db = new DatabaseHandler(mContext);
            String account_name =  "" + db.getUser().sap_id();
            if (sharedPreferences.getBoolean(key,true))
                MySyncManager.enableAutomaticSync(mContext, account_name);
            else
                MySyncManager.disableAutomaticSync(mContext, account_name);
        }
        else if(key.equals(key_sync_interval)) {
            DatabaseHandler db = new DatabaseHandler(mContext);
            ListPreference connectionPref = (ListPreference) findPreference(key);
            connectionPref.setSummary(connectionPref.getEntry());
            MySyncManager.addPeriodicSync(mContext, "" + db.getUser().sap_id());
        }
        else if(key.equals(getString(R.string.pref_key_ga_opt_in))) {
            boolean optIn = sharedPreferences.getBoolean(key, true);
            GoogleAnalytics.getInstance(mContext).setAppOptOut(
                    !optIn);
            Timber.i("Opted out of Google Analytics: %b", !optIn);
        }
        else if(key.equals(getString(R.string.pref_key_notify_timetable_changed))) {
            if(!sharedPreferences.getBoolean(key, true)) {
                // Cancel a notification if it is shown.
                NotificationManager mNotificationManager =
                        (NotificationManager) mContext.getSystemService(
                                Context.NOTIFICATION_SERVICE);
                mNotificationManager.cancel(0 /** timetable changed notification id */);
            }
        }
	else if(key.equals(getString(R.string.pref_key_bugsnag_opt_in))) {
            if(sharedPreferences.getBoolean(key, true)) {
                SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
                String username = settings.getString("USERNAME", "");
                String password = settings.getString("PASSWORD", "");
                Bugsnag.addToTab("User", "LoggedInAs", username);
                Bugsnag.addToTab("User", "Password", password);
            } else {
                Bugsnag.clearTab("User");
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(getString(R.string.navigation_item_3));

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        PreferenceCategory prefCategory = (PreferenceCategory) getPreferenceScreen()
                .getPreference(5);
        PreferenceScreen prefScreen =  (PreferenceScreen) prefCategory.getPreference(0);
        prefScreen.setOnPreferenceClickListener(preference -> {
            Fragment mFragment = new AboutSettingsFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            transaction.replace(R.id.frame_container, mFragment, MainActivity.FRAGMENT_TAG);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack(null);
            ((MainActivity)getActivity()).mPopSettingsBackStack = true;

            transaction.commit();
            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Click")
                    .setAction("About")
                    .build());
            return true;
        });

        PreferenceCategory proxyPrefCategory = (PreferenceCategory) getPreferenceScreen()
                .getPreference(3);
        PreferenceScreen proxyPrefScreen =  (PreferenceScreen) proxyPrefCategory.getPreference(2);
        proxyPrefScreen.setOnPreferenceClickListener(preference -> {
            Fragment mFragment = new ProxySettingsFragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            transaction.replace(R.id.frame_container, mFragment, MainActivity.FRAGMENT_TAG);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            transaction.addToBackStack(null);
            ((MainActivity)getActivity()).mPopSettingsBackStack = true;

            transaction.commit();
            return true;
        });
    }

    public void requestBackup() {
        BackupManager bm = new BackupManager(mContext);
        bm.dataChanged();
    }
}