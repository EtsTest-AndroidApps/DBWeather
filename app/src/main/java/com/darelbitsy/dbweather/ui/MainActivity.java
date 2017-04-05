package com.darelbitsy.dbweather.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;

import com.darelbitsy.dbweather.R;
import com.darelbitsy.dbweather.adapters.CustomFragmentAdapter;
import com.darelbitsy.dbweather.adapters.database.DatabaseOperation;
import com.darelbitsy.dbweather.adapters.listAdapter.HourAdapter;
import com.darelbitsy.dbweather.adapters.listAdapter.NewsAdapter;
import com.darelbitsy.dbweather.controller.api.adapters.helper.GetNewsesHelper;
import com.darelbitsy.dbweather.controller.api.adapters.helper.GetWeatherHelper;
import com.darelbitsy.dbweather.helper.holder.ConstantHolder;
import com.darelbitsy.dbweather.helper.services.LocationTracker;
import com.darelbitsy.dbweather.helper.utility.AppUtil;
import com.darelbitsy.dbweather.helper.utility.weather.WeatherUtil;
import com.darelbitsy.dbweather.model.geonames.GeoName;
import com.darelbitsy.dbweather.model.news.Article;
import com.darelbitsy.dbweather.model.weather.Weather;
import com.darelbitsy.dbweather.ui.animation.AnimationUtility;
import com.darelbitsy.dbweather.ui.animation.CubeOutTransformer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

import static com.darelbitsy.dbweather.helper.holder.ConstantHolder.FIRST_RUN;
import static com.darelbitsy.dbweather.helper.holder.ConstantHolder.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.darelbitsy.dbweather.helper.holder.ConstantHolder.MY_PERMiSSIONS_REQUEST_GET_ACCOUNT;
import static com.darelbitsy.dbweather.helper.holder.ConstantHolder.PREFS_NAME;
import static com.darelbitsy.dbweather.helper.holder.ConstantHolder.RECYCLER_BOTTOM_LIMIT;
import static com.darelbitsy.dbweather.helper.utility.weather.WeatherUtil.mColorPicker;

/**
 * Created by Darel Bitsy on 11/02/17.
 * MainActivity of the application
 * Handle location update and set viewPager
 */

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MenuItem.OnMenuItemClickListener {

    private DatabaseOperation mDatabase;
    private CustomFragmentAdapter mFragmentAdapter;
    private BroadcastReceiver mLocationBroadcast;
    private Single<Weather> mWeatherObservable;
    public static final CompositeDisposable subscriptions = new CompositeDisposable();
    private final Handler mUpdateHandler = new Handler();
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private NewsAdapter mNewsAdapter;
    private RecyclerView mNewsRecyclerView;
    private ArrayList<Article> mNewses;

    private Single<ArrayList<Article>> mNewsesObservableWithNetwork;
    private Single<ArrayList<Article>> mNewsesObservableWithoutNetwork;

    private boolean isSubscriptionDoneWithNetwork;
    private HourAdapter mHourAdapter;
    private final Handler mMyHandler = new Handler();
    private Weather mWeather;
    private SharedPreferences sharedPreferences;
    private View mainLayout;
    private final CompoundButton.OnCheckedChangeListener mNotificationConfigurationListener = (buttonView, isChecked) -> {
        if (isChecked) {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(ConstantHolder.NOTIFICATION_KEY, true)
                    .apply();

        } else {
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(ConstantHolder.NOTIFICATION_KEY, false)
                    .apply();
        }
    };

    private final CompoundButton.OnCheckedChangeListener mNewsConfigurationListener = (buttonView, isChecked) -> {
        if (isChecked) {
            sharedPreferences
                    .edit()
                    .putBoolean(ConstantHolder.NEWS_TRANSLATION_KEY, true)
                    .apply();

        } else {
            sharedPreferences
                    .edit()
                    .putBoolean(ConstantHolder.NEWS_TRANSLATION_KEY, false)
                    .apply();
        }
    };


    private void respondToMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.add_location_id) {
            startActivity(new Intent(this, AddLocationActivity.class));

        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        respondToMenuItemClick(item);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        respondToMenuItemClick(item);
        return true;
    }

    /**
     * This class implement the behavior
     * i want when i receive the weather data
     */
    private final class MainActivityWeatherObserver extends DisposableSingleObserver<Weather> {
        @Override
        public void onSuccess(Weather weather) {
            Log.i(ConstantHolder.TAG, "Inside the weatherObserver MainActivity");
            mWeather = weather;

            if (mFragmentAdapter != null) {
                Log.i(ConstantHolder.TAG, "Inside: fragmentAdapter not null");
                mUpdateHandler.post(() -> mFragmentAdapter.updateWeatherOnFragment(weather));

            } else {
                Log.i(ConstantHolder.TAG, "Inside: fragmentAdapter null");
            }
            if (mHourAdapter != null) {
                mHourAdapter.updateData(weather
                        .getHourly()
                        .getData());
            }
            Log.i(ConstantHolder.TAG, "City Name: "+ weather.getCityName());

            if (isSubscriptionDoneWithNetwork) {
                subscriptions.add(mNewsesObservableWithNetwork
                        .subscribeWith(new CurrentNewsesObserver()));

            } else {
                subscriptions.add(mNewsesObservableWithoutNetwork
                        .subscribeWith(new CurrentNewsesObserver()));
            }

        }

        @Override
        public void onError(Throwable e) {
            Log.i(ConstantHolder.TAG, "Error: " + e.getMessage());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDatabase = new DatabaseOperation(this);

        Bundle extras = getIntent().getExtras();
        mWeather = extras.getParcelable(ConstantHolder.WEATHER_DATA_KEY);
        mNewses = extras.getParcelableArrayList(ConstantHolder.NEWS_DATA_KEY);

        Toolbar toolbar = (Toolbar) findViewById(R.id.weatherToolbar);
        setSupportActionBar(toolbar);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        mainLayout = findViewById(R.id.dbweather_main_layout);
        mainLayout.setBackgroundResource(mColorPicker
                .getBackgroundColor(mWeather.getCurrently().getIcon()));

        mWeatherObservable = new GetWeatherHelper(this)
                .getObservableWeatherFromApi(mDatabase)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        AppUtil.askLocationPermIfNeeded(this);
        AppUtil.askAccountInfoPermIfNeeded(this);

        DbViewPager viewPager = (DbViewPager) findViewById(R.id.viewPager);
        
        mFragmentAdapter = new CustomFragmentAdapter(mainLayout, getSupportFragmentManager(),
                mWeather);

        viewPager.setAdapter(mFragmentAdapter);
        viewPager.setPageTransformer(false, new CubeOutTransformer());

        mDrawerLayout = (DrawerLayout) findViewById(R.id.weatherDrawerLayout);
        mDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
            }
        };

        setupNavigationDrawer();
        mDrawerLayout.addDrawerListener(mDrawerToggle);

        mLocationBroadcast = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                WeatherUtil.saveCoordinates(intent.getExtras().getDouble("latitude"),
                        intent.getExtras().getDouble("longitude"),
                        mDatabase);

                if (AppUtil.isNetworkAvailable(MainActivity.this)) {
                    subscriptions.add(mWeatherObservable
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeWith(new MainActivityWeatherObserver()));

                    isSubscriptionDoneWithNetwork = true;
                }
            }
        };

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mNewsesObservableWithNetwork = new GetNewsesHelper(this)
                .getNewsesFromApi()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        mNewsesObservableWithoutNetwork = new GetNewsesHelper(this)
                .getNewsesFromDatabase(mDatabase)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        if (sharedPreferences.getBoolean(FIRST_RUN, true)
                && AppUtil.isNetworkAvailable(this)) {

            subscriptions.add(mNewsesObservableWithNetwork
                    .subscribeWith(new CurrentNewsesObserver()));

            sharedPreferences
                    .edit()
                    .putBoolean(FIRST_RUN, false)
                    .apply();
        }

        mMyHandler.post(this::setupNewsScrollView);
        mMyHandler.post(this::setupHourlyRecyclerView);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerLayout.post(mDrawerToggle::syncState);
    }

    private void setupHourlyRecyclerView() {
        mHourAdapter = new HourAdapter(mWeather.getHourly().getData());
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.hourlyRecyclerView);
        recyclerView.setAdapter(mHourAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL,
                false));


        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay()
                .getRealMetrics(metrics);

        float height = metrics.heightPixels;

        sharedPreferences.edit()
                .putFloat(RECYCLER_BOTTOM_LIMIT, Math.round(height * 0.7f))
                .apply();
    }

    private void setupNavigationDrawer() {
        NavigationView navigationView = (NavigationView)
                mDrawerLayout.findViewById(R.id.navigationView);
        navigationView.setNavigationItemSelectedListener(this);
        List<GeoName> listOfLocation = mDatabase.getUserCitiesFromDatabase();

        Menu menu = navigationView.getMenu();
        Menu locationSubmenu = menu.findItem(R.id.location_config_id)
                .setOnMenuItemClickListener(this)
                .setEnabled(true)
                .getSubMenu();

        final MenuItem addLocationItem =
                locationSubmenu.findItem(R.id.add_location_id);
        addLocationItem.setOnMenuItemClickListener(this);

        for (int index = 0; index < listOfLocation.size(); index++) {
            GeoName location = listOfLocation.get(index);
            MenuItem item = locationSubmenu.add(R.id.cities_menu_id, index + 1, Menu.NONE,
                    location.getName() + ", " + location.getCountryName());
            item.setIcon(R.drawable.city_location_icon);
            item.setOnMenuItemClickListener(this);
            item.setEnabled(true);
        }

        SwitchCompat notification_switch = (SwitchCompat)
                MenuItemCompat.getActionView(menu.findItem(R.id.notification_config_id));
        SwitchCompat news_translation_switch = (SwitchCompat)
                MenuItemCompat.getActionView(menu.findItem(R.id.news_translation_config_id));

        notification_switch.setOnCheckedChangeListener(mNotificationConfigurationListener);
        news_translation_switch.setOnCheckedChangeListener(mNewsConfigurationListener);

        notification_switch.setChecked(sharedPreferences.getBoolean(ConstantHolder.NEWS_TRANSLATION_KEY, false));
        news_translation_switch.setChecked(sharedPreferences.getBoolean(ConstantHolder.NEWS_TRANSLATION_KEY, false));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mLocationBroadcast,
                new IntentFilter("dbweather_location_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscriptions.dispose();
        if (mLocationBroadcast != null) {
            unregisterReceiver(mLocationBroadcast);
        }
        cleanCache();
    }

    private void cleanCache() {
        File dir = AppUtil.getFileCache(this);
        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                Log.i(ConstantHolder.TAG, "Is File Cache Cleared on exit: "
                        + file.delete());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Checking if the user cancelled, the permission
        if(requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                && (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

            AppUtil.setGpsPermissionValue(this);
            startService(new Intent(this, LocationTracker.class));
            mainLayout.setBackgroundResource(mColorPicker
                    .getBackgroundColor(mWeather.getCurrently().getIcon()));

        }

        if (requestCode == MY_PERMiSSIONS_REQUEST_GET_ACCOUNT
                && (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

            AppUtil.setAccountPermissionValue(this);

        }
        setupNewsScrollView();
    }

    private final class CurrentNewsesObserver extends DisposableSingleObserver<ArrayList<Article>> {
        @Override
        public void onSuccess(ArrayList<Article> newses) {
            Log.i(ConstantHolder.TAG, "Inside the currentNewsesObserver Fragment");
            mNewses = newses;
            if (mNewsAdapter != null) {
                new Handler().post(() -> mNewsAdapter.updateContent(newses));
            }
        }

        @Override
        public void onError(Throwable e) {
            Log.i(ConstantHolder.TAG, "Ho My God, got an error: " + e.getMessage());
        }
    }

    // Setup the news scroll view and fetch it with data if available
    private void setupNewsScrollView() {
        if (mNewsRecyclerView == null) {
            mNewsRecyclerView = (RecyclerView)
                    findViewById(R.id.newsRecyclerView);
        }

        if(mNewses != null && !mNewses.isEmpty()) {
            if (mNewsAdapter ==  null) {
                mNewsAdapter = new NewsAdapter(mNewses);
                mNewsRecyclerView.setAdapter(mNewsAdapter);
                LinearLayoutManager layoutManager = new LinearLayoutManager(this,
                        LinearLayoutManager.HORIZONTAL,
                        false) {

                    @Override
                    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
                        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
                            private static final float SPEED = 4500f;// Change this value (default=25f)

                            @Override
                            protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                                return SPEED / displayMetrics.densityDpi;
                            }
                        };

                        smoothScroller.setTargetPosition(position);
                        startSmoothScroll(smoothScroller);
                    }

                    @Override
                    public boolean supportsPredictiveItemAnimations() {
                        return true;
                    }
                };

                mNewsRecyclerView.setLayoutManager(layoutManager);
                mNewsRecyclerView.setHasFixedSize(true);

            }
            AnimationUtility.autoScrollRecyclerView(mNewsRecyclerView, mNewsAdapter);
        }
    }
}