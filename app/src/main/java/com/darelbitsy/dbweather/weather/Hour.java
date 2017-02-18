package com.darelbitsy.dbweather.weather;

import android.os.Parcel;
import android.os.Parcelable;

import com.darelbitsy.dbweather.WeatherApi;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by Darel Bitsy on 07/01/17.
 */

public class Hour extends WeatherData implements Parcelable {


    /**
     * Empty constructor just like place holder
     */
    public Hour() {}

    public static final Creator<Hour> CREATOR = new Creator<Hour>() {
        @Override
        public Hour createFromParcel(Parcel in) { return new Hour(in); }
        @Override
        public Hour[] newArray(int size) { return new Hour[size]; }
    };



    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(getTemperature());
        dest.writeLong(getTime());
        dest.writeString(getSummary());
        dest.writeString(getIcon());
        dest.writeString(getTimeZone());
    }

    private Hour(Parcel in) {
        setTemperature(in.readInt());
        setTime(in.readLong());
        setSummary(in.readString());
        setIcon(in.readString());
        setTimeZone(in.readString());
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "HOUR{ICON:%s ; SUMMARY:%s ; TIME:%d ; TIMEZONE: %s; TEMPERATURE:%d}",
                getIcon(),
                getSummary(),
                getTime(),
                getTimeZone(),
                getTemperature());
    }

    public String getHour() {
        return getHour(getTime());
    }

    public String getHour(long timeInMilliseconds) {
        final DateTimeFormatter format =
                DateTimeFormatter.ofPattern("h a");

        return Instant.ofEpochSecond(timeInMilliseconds)
                .atZone(ZoneId.of(getTimeZone() == null ? TimeZone.getDefault().getID() : getTimeZone()))
                .format(format);
    }
}
