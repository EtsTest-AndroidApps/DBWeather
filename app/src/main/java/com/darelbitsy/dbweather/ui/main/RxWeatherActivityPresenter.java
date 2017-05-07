package com.darelbitsy.dbweather.ui.main;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.util.Log;

import com.darelbitsy.dbweather.models.datatypes.geonames.GeoName;
import com.darelbitsy.dbweather.models.datatypes.news.Article;
import com.darelbitsy.dbweather.models.datatypes.weather.HourlyData;
import com.darelbitsy.dbweather.models.datatypes.weather.WeatherInfo;
import com.darelbitsy.dbweather.models.provider.AppDataProvider;
import com.darelbitsy.dbweather.models.provider.repository.IUserCitiesRepository;
import com.darelbitsy.dbweather.models.provider.schedulers.ISchedulersProvider;
import com.darelbitsy.dbweather.models.provider.schedulers.RxSchedulersProvider;
import com.darelbitsy.dbweather.utils.holder.ConstantHolder;
import com.darelbitsy.dbweather.utils.utility.AppUtil;
import com.darelbitsy.dbweather.utils.utility.weather.WeatherUtil;

import java.io.File;
import java.util.List;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;

/**
 * Created by Darel Bitsy on 22/04/17.
 * MainView Presenter with Rx
 */

public class RxWeatherActivityPresenter {

    private final ISchedulersProvider mSchedulersProvider;
    private final IUserCitiesRepository mUserCitiesRepository;
    private final IWeatherActivityView mMainView;
    private final AppDataProvider mDataProvider;
    private final CompositeDisposable rxSubscriptions = new CompositeDisposable();


    RxWeatherActivityPresenter(
            final IUserCitiesRepository userCitiesRepository,
            final IWeatherActivityView mainView,
            final AppDataProvider dataProvider) {

        mUserCitiesRepository = userCitiesRepository;

        mMainView = mainView;

        mDataProvider = dataProvider;

        mSchedulersProvider = RxSchedulersProvider.newInstance();
    }

    void configureView() {
        loadUserCitiesMenu();
        loadWeather();
        loadNews();
    }

    private void loadWeather() {
        rxSubscriptions.add(mDataProvider.getWeatherFromDatabase()
                .subscribeOn(mSchedulersProvider.getWeatherScheduler())
                .observeOn(mSchedulersProvider.getComputationThread())
                .map(weather -> WeatherUtil.parseWeather(weather, mMainView.getAppContext()))
                .observeOn(mSchedulersProvider.getUIScheduler())
                .subscribeWith(new WeatherObserver()));
    }

    void loadNews() {
        rxSubscriptions.add(mDataProvider.getNewsFromDatabase()
                .subscribeOn(mSchedulersProvider.getNewsScheduler())
                .observeOn(mSchedulersProvider.getUIScheduler())
                .subscribeWith(new NewsObserver()));
    }

    void clearState() {
        rxSubscriptions.clear();
        cleanCache(mMainView.getAppContext());
    }

    public void getWeather() {
        rxSubscriptions.add(mDataProvider.getWeatherFromApi()
                .subscribeOn(mSchedulersProvider.getWeatherScheduler())
                .observeOn(mSchedulersProvider.getComputationThread())
                .map(weather -> WeatherUtil.parseWeather(weather, mMainView.getAppContext()))
                .observeOn(mSchedulersProvider.getUIScheduler())
                .subscribeWith(new WeatherObserver()));
    }

    void getWeatherForCity(@NonNull final String cityName,
                           final double latitude,
                           final double longitude) {

        rxSubscriptions.add(mDataProvider.getWeatherForCityFromApi(cityName, latitude, longitude)
                .subscribeOn(mSchedulersProvider.getWeatherScheduler())
                .observeOn(mSchedulersProvider.getComputationThread())
                .map(weather -> WeatherUtil.parseWeather(weather, mMainView.getAppContext()))
                .observeOn(mSchedulersProvider.getUIScheduler())
                .subscribeWith(new WeatherObserver()));
    }

    void loadWeatherForCity(@NonNull final String cityName,
                            final double latitude,
                            final double longitude) {

        rxSubscriptions.add(mDataProvider.getWeatherForCityFromDatabase(cityName, latitude, longitude)
                .subscribeOn(mSchedulersProvider.getWeatherScheduler())
                .observeOn(mSchedulersProvider.getComputationThread())
                .map(weather -> WeatherUtil.parseWeather(weather, mMainView.getAppContext()))
                .observeOn(mSchedulersProvider.getUIScheduler())
                .subscribeWith(new WeatherObserver()));
    }

    void loadUserCitiesMenu() {

        rxSubscriptions.add(mUserCitiesRepository.getUserCities()
                .subscribeOn(mSchedulersProvider.getDatabaseWorkScheduler())
                .observeOn(mSchedulersProvider.getUIScheduler())
                .subscribeWith(new DisposableSingleObserver<List<GeoName>>() {
                    @Override
                    public void onSuccess(@NonNull final List<GeoName> userCities) {
                        if (userCities.isEmpty()) {
                            mMainView.setupNavigationDrawerWithNoCities();

                        } else {
                            mMainView.setupNavigationDrawerWithCities(userCities);
                        }
                    }
                    @Override
                    public void onError(final Throwable throwable) {
                        mMainView.setupNavigationDrawerWithNoCities();
                    }
                }));
    }

    void removeCityFromUserCities(@NonNull final GeoName location) {
        mUserCitiesRepository.removeCity(location);
    }

    public void getNews() {
        rxSubscriptions.add(mDataProvider.getNewsFromApi()
                .subscribeOn(mSchedulersProvider.getNewsScheduler())
                .observeOn(mSchedulersProvider.getUIScheduler())
                .subscribeWith(new NewsObserver()));
    }

    boolean isFirstRun() {
        return mDataProvider.isFirstRun();
    }

    void setFirstRun(final boolean isFirstRun) {
        mDataProvider.setFirstRun(isFirstRun);
    }

    boolean didUserSelectedCityFromDrawer() {
        return mDataProvider.didUserSelectedCityFromDrawer();
    }

    void userSelectedCityFromDrawer(final boolean isFromCity) {
        mDataProvider.userSelectedCityFromDrawer(isFromCity);
    }

    Pair<String, double[]> getSelectedUserCity(@NonNull final String locationToFind) {
        return mDataProvider.getSelectedUserCity(locationToFind);
    }

    void saveRecyclerBottomLimit(final float limit) {
        mDataProvider.saveRecyclerBottomLimit(limit);
    }

    boolean getWeatherNotificationStatus() {
        return mDataProvider.getWeatherNotificationStatus();
    }

    void setWeatherNotificationStatus(final boolean isOn) {
        mDataProvider.setWeatherNotificationStatus(isOn);
    }

    boolean getNewsTranslationStatus() {
        return mDataProvider.getNewsTranslationStatus();
    }

    void setNewsTranslationStatus(final boolean isOn) {
        mDataProvider.setNewsTranslationStatus(isOn);
    }

    boolean getWritePermissionStatus() {
        return mDataProvider.getWritePermissionStatus();
    }

    void setSelectedUserCity(@NonNull final String locationSelected,
                             final double latitude,
                             final double longitude) {

        mDataProvider.setSelectedUserCity(locationSelected, latitude, longitude);
    }

    private void cleanCache(@NonNull final Context context) {
        final File dir = AppUtil.getFileCache(context);
        if (dir.isDirectory()) {
            for (final File file : dir.listFiles()) {
                Log.i(ConstantHolder.TAG, "Is File Cache Cleared on exit: "
                        + file.delete());
            }
        }
    }

    private class NewsObserver extends DisposableSingleObserver<List<Article>> {
        @Override
        public void onSuccess(@NonNull final List<Article> articles) {
            if (!articles.isEmpty()) {
                mMainView.showNews(articles);

            } else {
                mMainView.showNetworkNewsErrorMessage();
            }
        }

        @Override
        public void onError(final Throwable throwable) {
            mMainView.showNetworkNewsErrorMessage();
        }
    }

    private class WeatherObserver extends DisposableSingleObserver<Pair<List<WeatherInfo>,List<HourlyData>>> {

        @Override
        public void onSuccess(@NonNull final Pair<List<WeatherInfo>,List<HourlyData>> weather) {
            if ((weather.first != null && !weather.first.isEmpty()) &&
                    (weather.second != null && !weather.second.isEmpty())) {

                mMainView.showWeather(weather);

            } else {
                mMainView.showNetworkWeatherErrorMessage();
            }
        }

        @Override
        public void onError(final Throwable throwable) {
            mMainView.showNetworkWeatherErrorMessage();
        }
    }

    CompositeDisposable getRxSubscriptions() {
        return rxSubscriptions;
    }

    ISchedulersProvider getSchedulersProvider() {
        return mSchedulersProvider;
    }
}