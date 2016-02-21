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

package com.shalzz.attendance.controllers;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.bugsnag.android.BeforeNotify;
import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Error;
import com.shalzz.attendance.BuildConfig;
import com.shalzz.attendance.DatabaseHandler;
import com.shalzz.attendance.Miscellaneous;
import com.shalzz.attendance.R;
import com.shalzz.attendance.activity.LoginActivity;
import com.shalzz.attendance.activity.MainActivity;
import com.shalzz.attendance.model.UserModel;
import com.shalzz.attendance.network.DataAPI;
import com.shalzz.attendance.wrapper.MyPreferencesManager;
import com.shalzz.attendance.wrapper.MySyncManager;
import com.shalzz.attendance.wrapper.MyVolley;
import com.shalzz.attendance.wrapper.MyVolleyErrorHelper;


public class UserAccount {

    private Miscellaneous misc;

    /**
     * The activity context used to Log the user from
     */
    private Context mContext;

    /**
     * Constructor to set the Activity context.
     * @param context Context
     */
    public UserAccount(Context context) {
        mContext = context;
        misc =  new Miscellaneous(mContext);
    }

    /**
     * Sends the login request and saves the user details.
     * @param username Username
     * @param password Password
     */
    public void Login(final String username, final String password) {

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean optIn = sharedPref.getBoolean(mContext.getString(
                R.string
                .pref_key_bugsnag_opt_in), true);
        if(optIn) {
            Bugsnag.beforeNotify(new BeforeNotify() {
                public boolean run(Error error) {
                    SharedPreferences settings = mContext.getSharedPreferences("SETTINGS", 0);
                    String username = settings.getString("USERNAME", "");
                    String password = settings.getString("PASSWORD", "");
                    Bugsnag.addToTab("User", "Username", username);
                    Bugsnag.addToTab("User", "Password", password);
                    return true;
                }
            });
        }

        if(BuildConfig.DEBUG)
            Log.d("User Account",Miscellaneous.md5(password));
        String creds = String.format("%s:%s", username, Miscellaneous.md5(password));
        misc.showProgressDialog("Logging in...", false, pdCancelListener());
        DataAPI.getUser( loginSuccessListener(), myErrorListener(), creds);
    }

    private Response.Listener<UserModel> loginSuccessListener() {
        return new Response.Listener<UserModel>() {
            @Override
            public void onResponse(UserModel user) {

                Bugsnag.leaveBreadcrumb("Logged in Successfully");
                MyPreferencesManager.saveUser(user.getSapid(), user.getPassword());
                MySyncManager.addPeriodicSync(mContext, user.getSapid());
                DatabaseHandler db = new DatabaseHandler(mContext);
                db.addOrUpdateUser(user);
                db.close();

                misc.dismissProgressDialog();
                Intent ourIntent = new Intent(mContext, MainActivity.class);
                mContext.startActivity(ourIntent);
                ((Activity) mContext).finish();
            }
        };
    }

    /**
     * Progress Dialog cancel Listener.
     * @return OnCancelListener
     */
    DialogInterface.OnCancelListener pdCancelListener() {
        return new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // Cancel all pending requests when user presses back button.
                MyVolley.getInstance().cancelPendingRequests(mContext.getClass().getName());
                MyVolley.getInstance().cancelPendingRequests("LOGOUT");
            }
        };

    }

    private Response.ErrorListener myErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Bugsnag.clearTab("User");
                String msg = MyVolleyErrorHelper.getMessage(error, mContext);
                misc.dismissProgressDialog();
                View view = ((Activity) mContext).getCurrentFocus();
                Miscellaneous.showSnackBar(view, msg);
                Log.e(mContext.getClass().getName(), msg);
            }
        };
    }

    /**
     * Sends the Logout request, clears the user details preferences and deletes all user attendance data.
     */
    public void Logout() {
        Bugsnag.leaveBreadcrumb("User logged out");
        MainActivity.LOGGED_OUT = true;

        // Remove UserModel Details from Shared Preferences.
        MyPreferencesManager.removeUser();

        // Remove user Attendance data from database.
        DatabaseHandler db = new DatabaseHandler(mContext);
        db.resetTables();

        // Remove Sync Account
        MySyncManager.removeSyncAccount(mContext);

        // Cancel a notification if it is shown.
        NotificationManager mNotificationManager =
                (NotificationManager) mContext.getSystemService(
                        Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0 /** timetable changed notification id */);

        // Destroy current activity and start Login Activity
        Intent ourIntent = new Intent(mContext, LoginActivity.class);
        mContext.startActivity(ourIntent);
        ((Activity) mContext).finish();
    }
}
