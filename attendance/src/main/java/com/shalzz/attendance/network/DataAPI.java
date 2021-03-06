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

package com.shalzz.attendance.network;

import com.shalzz.attendance.model.remote.Period;
import com.shalzz.attendance.model.remote.Subject;
import com.shalzz.attendance.model.remote.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface DataAPI {

    String ENDPOINT = "https://upes.winnou.net/api/v1/";

    @GET("me")
    Call<User> getUser(@Header("Authorization") String authorization);

    @GET("me/attendance")
    Call<List<Subject>> getAttendance();

    @GET("me/attendance")
    Call<List<Subject>> getAttendance(@Header("Authorization") String authorization);

    @GET("me/timetable")
    Call<List<Period>> getTimetable();

    @GET("me/timetable")
    Call<List<Period>> getTimetable(@Header("Authorization") String authorization);
}
