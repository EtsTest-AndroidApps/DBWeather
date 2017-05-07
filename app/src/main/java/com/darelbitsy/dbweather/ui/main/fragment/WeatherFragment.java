package com.darelbitsy.dbweather.ui.main.fragment;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.darelbitsy.dbweather.R;
import com.darelbitsy.dbweather.databinding.FragmentWeatherBinding;
import com.darelbitsy.dbweather.ui.BaseActivity;
import com.darelbitsy.dbweather.utils.helper.ColorManager;
import com.darelbitsy.dbweather.utils.holder.ConstantHolder;
import com.darelbitsy.dbweather.models.datatypes.weather.WeatherInfo;
import com.darelbitsy.dbweather.utils.utility.weather.WeatherUtil;

import static com.darelbitsy.dbweather.utils.holder.ConstantHolder.REQUEST_WEATHER;
import static com.darelbitsy.dbweather.utils.holder.ConstantHolder.WEATHER_INFO_KEY;

/**
 * Created by Darel Bitsy on 24/04/17.
 * Fragment Representing weather info
 */

public class WeatherFragment extends Fragment implements IWeatherFragmentView<WeatherInfo> {

    private WeatherFragmentPresenter mPresenter;
    private FragmentWeatherBinding mFragmentWeatherBinding;
    private RelativeLayout.LayoutParams mParams;

    public static WeatherFragment newInstance(@NonNull final WeatherInfo weatherInfo) {

        final WeatherFragment weatherFragment = new WeatherFragment();

        final Bundle args = new Bundle();
        args.putParcelable(WEATHER_INFO_KEY, weatherInfo);
        weatherFragment.setArguments(args);

        return weatherFragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPresenter = new WeatherFragmentPresenter(this);
        final Bundle arguments = getArguments();

        if (arguments != null) {
            mPresenter.restoreState(arguments);
        }

        if (savedInstanceState != null) {
            mPresenter.restoreState(savedInstanceState);

        }

        mParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(WEATHER_INFO_KEY, mPresenter.getWeatherInfo());
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {

        mFragmentWeatherBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_weather, container, false);
        mFragmentWeatherBinding.setPresenter(mPresenter);
        mFragmentWeatherBinding.currentWeatherLayout.setBackgroundResource(ColorManager.newInstance()
                .getBackgroundColor(mPresenter.getWeatherInfo()
                        .icon.get()));
        return mFragmentWeatherBinding.getRoot();
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mPresenter.getWeatherInfo().isCurrentWeather.get()) {
            mPresenter.showFallingSnowOrRain(mFragmentWeatherBinding.currentWeatherLayout,
                    getActivity().getApplicationContext(), mParams);
        }

        mFragmentWeatherBinding.refreshLayout.setOnRefreshListener(this::requestUpdate);
    }

    @Override
    public void showData(@NonNull final WeatherInfo weatherInfo) {
        mPresenter.showData(weatherInfo);
        if (mFragmentWeatherBinding.refreshLayout.isRefreshing()) {
            mFragmentWeatherBinding.refreshLayout.setRefreshing(false);
        }
        mFragmentWeatherBinding.currentWeatherLayout.setBackgroundResource(ColorManager.newInstance()
                .getBackgroundColor(mPresenter.getWeatherInfo()
                        .icon.get()));

        if (mPresenter.getWeatherInfo().isCurrentWeather.get()) {
            mPresenter.showFallingSnowOrRain(mFragmentWeatherBinding.currentWeatherLayout,
                    mFragmentWeatherBinding.currentWeatherLayout.getContext(),
                    mParams);
        }

    }

    @Override
    public void requestUpdate() {
        final Intent updateRequest = new Intent(ConstantHolder.UPDATE_REQUEST);
        final BaseActivity activity = (BaseActivity) getActivity();
        final Bundle firebaseLog = new Bundle();

        firebaseLog.putString(REQUEST_WEATHER, String.format("Request weather update at %s", WeatherUtil.getHour(System.currentTimeMillis(), null)));
        activity.mAnalyticProvider.logEvent(REQUEST_WEATHER, firebaseLog);
        activity.sendBroadcast(updateRequest);
    }
}
