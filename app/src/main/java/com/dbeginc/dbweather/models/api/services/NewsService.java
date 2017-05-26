package com.dbeginc.dbweather.models.api.services;

import com.dbeginc.dbweather.models.datatypes.news.NewsResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by Darel Bitsy on 18/02/17.
 */

public interface NewsService {
    @GET("/v1/articles")
    Call<NewsResponse> getNewsFromApiSync(
            @Query("source")
            final String source,
            @Query("sortBy")
            final String sortBy,
            @Query("apiKey")
            final String apiKey
    );
}