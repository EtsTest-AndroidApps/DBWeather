/*
 *  Copyright (C) 2017 Darel Bitsy
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package com.dbeginc.dbweatherdata.implementations.datasources.local.weather

import android.content.Context
import android.support.annotation.RestrictTo
import com.dbeginc.dbweatherdata.implementations.datasources.local.LocalWeatherDataSource
import com.dbeginc.dbweatherdata.implementations.datasources.local.weather.room.LocalCurrentWeatherDatabase
import com.dbeginc.dbweatherdata.implementations.datasources.local.weather.room.LocalLocationWeatherDatabase
import com.dbeginc.dbweatherdata.proxies.local.weather.*
import com.dbeginc.dbweatherdata.proxies.mappers.toDomain
import com.dbeginc.dbweatherdomain.entities.requests.weather.LocationRequest
import com.dbeginc.dbweatherdomain.entities.requests.weather.WeatherRequest
import com.dbeginc.dbweatherdomain.entities.weather.*
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import org.jetbrains.annotations.TestOnly

/**
 * Created by darel on 15.09.17.
 *
 * Local Weather Data Source
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class LocalWeatherDataSourceImpl private constructor(private val currentDatabase: LocalCurrentWeatherDatabase,
                                                     private val locationDatabase: LocalLocationWeatherDatabase) : LocalWeatherDataSource {

    companion object {
        fun create(context: Context) : LocalWeatherDataSource {
            return LocalWeatherDataSourceImpl(
                    LocalCurrentWeatherDatabase.createDb(context),
                    LocalLocationWeatherDatabase.createDb(context)
            )
        }

        @TestOnly
        @RestrictTo(RestrictTo.Scope.TESTS)
        fun create(current: LocalCurrentWeatherDatabase, locations: LocalLocationWeatherDatabase) = LocalWeatherDataSourceImpl(current, locations)

    }

    override fun getWeather(request: WeatherRequest<String>): Maybe<Weather> {
        return currentDatabase.weatherDao().getWeatherByLocation(request.arg)
                .map { proxy -> proxy.toDomain() }
    }

    override fun getWeatherForLocation(locationName: String): Single<Weather> {
        return locationDatabase.weatherDao().getWeatherByLocation(locationName)
                .map { proxy -> proxy.toDomain() }
                .toSingle()
    }

    override fun getLocations(request: LocationRequest): Maybe<List<Location>> {
        return locationDatabase.weatherDao().getLocations(request.query)
                .map { locations -> locations.map { location -> location.toDomain() } }
    }

    override fun getUserLocations(): Maybe<List<Location>> {
        return locationDatabase.weatherDao().getUserLocations()
                .map { locations -> locations.map { location -> location.toDomain() } }
    }

    override fun updateWeather(weather: Weather): Completable {
        return Completable.fromAction {
            currentDatabase.weatherDao().deleteAll()
            currentDatabase.weatherDao().putWeather(weather.toProxy())
        }
    }

    override fun updateWeatherLocation(weather: Weather): Completable  {
        return Completable.fromAction {
            locationDatabase.weatherDao().putWeather(weather.toProxy())
        }
    }

    override fun deleteWeatherForLocation(weather: Weather): Completable {
        return Completable.fromAction {
            locationDatabase.weatherDao().deleteWeather(weather.toProxy())
        }
    }

    private fun Location.toProxy() : LocalLocation {
        return LocalLocation(name, latitude, longitude, countryCode, countryName)
    }

    private fun Weather.toProxy() : LocalWeather {
        return LocalWeather(location.toProxy(), latitude, longitude, timezone,
                currently.toProxy(), minutely?.toProxy(), hourly.toProxy(),
                daily.toProxy(), alerts?.map { alert -> alert.toProxy() }, flags.toProxy()
        )
    }

    private fun Minutely.toProxy() : LocalMinutely {
        return LocalMinutely(summary= summary, icon= icon, data= data.map { minutelyData -> minutelyData.toProxy() })
    }

    private fun Hourly.toProxy() : LocalHourly {
        return LocalHourly(summary= summary, icon= icon, data= data.map { hourlyData -> hourlyData.toProxy() })
    }

    private fun Daily.toProxy() : LocalDaily {
        return LocalDaily(summary= summary, icon= icon, data = data.map { dailyData -> dailyData.toProxy() })
    }

    private fun Alert.toProxy() : LocalAlert {
        return LocalAlert(time= time, title= title, description= description,
                uri= uri, expires= expires, regions= regions, severity= severity
        )
    }

    private fun Flags.toProxy() : LocalFlags {
        return LocalFlags(sources= sources, units= units)
    }

    private fun MinutelyData.toProxy() : LocalMinutelyData {
        return LocalMinutelyData(time= time, precipProbability= precipIntensity, precipIntensity= precipProbability)
    }

    private fun HourlyData.toProxy() : LocalHourlyData {
        return LocalHourlyData(time, summary, icon, temperature, apparentTemperature, dewPoint,
                humidity, pressure, windSpeed, windGust, windBearing, cloudCover,
                precipIntensity, precipProbability, precipType, uvIndex, ozone
        )
    }

    private fun DailyData.toProxy() : LocalDailyData {
        return LocalDailyData(time, summary, icon, temperatureHigh, temperatureHighTime,
                temperatureLow, temperatureLowTime, apparentTemperatureHigh, apparentTemperatureHighTime,
                apparentTemperatureLow, apparentTemperatureLowTime, dewPoint,
                humidity, pressure, windSpeed, windGust, windGustTime,
                windBearing, cloudCover, moonPhase, visibility,
                uvIndex, uvIndexTime, sunsetTime, sunriseTime,
                precipIntensity, precipIntensityMax, precipProbability, precipType
        )
    }

    private fun Currently.toProxy() : LocalCurrently {
        return LocalCurrently(time = time, summary = summary, icon = icon, temperature = temperature,
                apparentTemperature = apparentTemperature, precipIntensity = precipIntensity, precipIntensityError = precipIntensityError,
                precipProbability = precipProbability, precipType = precipType, pressure = pressure,
                cloudCover = cloudCover, dewPoint = dewPoint, humidity = humidity, nearestStormDistance = nearestStormDistance,
                nearestStormBearing = nearestStormBearing, windSpeed = windSpeed, windBearing = windBearing, visibility = visibility
        )
    }

}