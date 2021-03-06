package com.shalzz.attendance.ui.day;

import com.shalzz.attendance.model.local.Day;
import com.shalzz.attendance.ui.base.MvpView;

/**
 * @author shalzz
 */

interface DayMvpView extends MvpView {

    void clearDay();

    void setDay(Day day);
}
