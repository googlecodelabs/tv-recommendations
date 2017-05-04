/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.tv.recommendations.model;

import android.content.Context;
import android.support.annotation.Nullable;
import com.example.android.tv.recommendations.R;
import com.example.android.tv.recommendations.util.SharedPreferencesHelper;
import java.util.Arrays;
import java.util.List;

/** Mock database stores data in {@link android.content.SharedPreferences}. */
public final class MockDatabase {

    private MockDatabase() {
        // Do nothing.
    }

    public static List<Subscription> createUniversalSubscriptions(Context context) {

        String newForYou = context.getString(R.string.new_for_you);
        Subscription flagshipSubscription =
                Subscription.createSubscription(
                        newForYou,
                        context.getString(R.string.new_for_you_description),
                        buildIntentLinkUri(context, newForYou),
                        R.drawable.app_icon_your_company);

        String trendingVideos = context.getString(R.string.trending_videos);
        Subscription videoSubscription =
                Subscription.createSubscription(
                        trendingVideos,
                        context.getString(R.string.trending_videos_description),
                        buildIntentLinkUri(context, trendingVideos),
                        R.drawable.app_icon_quantum_card);

        String featuredFilms = context.getString(R.string.featured_films);
        Subscription filmsSubscription =
                Subscription.createSubscription(
                        featuredFilms,
                        context.getString(R.string.featured_films_description),
                        buildIntentLinkUri(context, featuredFilms),
                        R.drawable.videos_by_google_icon);

        return Arrays.asList(flagshipSubscription, videoSubscription, filmsSubscription);
    }

    private static String buildIntentLinkUri(Context context, String name) {
        return context.getString(
                R.string.app_link_view,
                context.getString(R.string.schema),
                context.getString(R.string.host),
                name);
    }

    public static Subscription getTvShowSubscription(Context context) {

        // See if we have already created the channel in the TV provider.
        String title = context.getString(R.string.title_tv_shows);
        Subscription subscription = findSubscriptionByTitle(context, title);
        if (subscription != null) {
            return subscription;
        }
        return Subscription.createSubscription(
                title,
                context.getString(R.string.tv_shows_description),
                buildIntentLinkUri(context, title),
                R.drawable.videos_by_google_icon);
    }

    public static Subscription getVideoSubscription(Context context) {

        // See if we have already created the channel in the TV provider.
        String title = context.getString(R.string.your_videos);

        Subscription subscription = findSubscriptionByTitle(context, title);
        if (subscription != null) {
            return subscription;
        }

        return Subscription.createSubscription(
                title,
                context.getString(R.string.your_videos_description),
                buildIntentLinkUri(context, title),
                R.drawable.videos_by_google_icon);
    }

    public static Subscription getRandomSubscription(Context context) {

        // See if we have already created the channel in the TV provider.
        String title = context.getString(R.string.random);
        Subscription subscription = findSubscriptionByTitle(context, title);
        if (subscription != null) {
            return subscription;
        }

        return Subscription.createSubscription(
                title,
                context.getString(R.string.random_description),
                buildIntentLinkUri(context, title),
                R.drawable.videos_by_google_icon);
    }

    @Nullable
    private static Subscription findSubscriptionByTitle(Context context, String title) {
        for (Subscription subscription : getSubscriptions(context)) {
            if (subscription.getName().equals(title)) {
                return subscription;
            }
        }
        return null;
    }

    public static void saveSubscriptions(Context context, List<Subscription> subscriptions) {
        SharedPreferencesHelper.storeSubscriptions(context, subscriptions);
    }

    public static List<Subscription> getSubscriptions(Context context) {
        return SharedPreferencesHelper.readSubscriptions(context);
    }

    @Nullable
    public static Subscription findSubscriptionByChannelId(Context context, long channelId) {
        for (Subscription subscription : getSubscriptions(context)) {
            if (subscription.getChannelId() == channelId) {
                return subscription;
            }
        }
        return null;
    }

    @Nullable
    public static Subscription findSubscriptionByName(Context context, String name) {
        for (Subscription subscription : getSubscriptions(context)) {
            if (subscription.getName().equals(name)) {
                return subscription;
            }
        }
        return null;
    }

    public static void saveMovies(Context context, long channelId, List<Movie> programs) {
        SharedPreferencesHelper.storeMovies(context, channelId, programs);
    }

    /**
     * Finds movie in subscriptions with channel id and updates it. Otherwise will add the new movie
     * to the subscription.
     *
     * @param context to access shared preferences.
     * @param channelId of the subscription that the movie is associated with.
     * @param movie to be persisted or updated.
     */
    public static void saveMovie(Context context, long channelId, Movie movie) {
        List<Movie> movies = getMovies(context, channelId);
        int index = findMovie(movies, movie);
        if (index == -1) {
            movies.add(movie);
        } else {
            movies.set(index, movie);
        }
        saveMovies(context, channelId, movies);
    }

    private static int findMovie(List<Movie> movies, Movie movie) {
        for (int index = 0; index < movies.size(); ++index) {
            Movie current = movies.get(index);
            if (current.getId() == movie.getId()) {
                return index;
            }
        }
        return -1;
    }

    public static List<Movie> getMovies(Context context, long channelId) {
        return SharedPreferencesHelper.readMovies(context, channelId);
    }

    @Nullable
    public static Movie findMovieByTitle(Context context, long channelId, String title) {
        for (Movie movie : getMovies(context, channelId)) {
            if (movie.getTitle().equals(title)) {
                return movie;
            }
        }
        return null;
    }

    @Nullable
    public static Movie findMovieById(Context context, long channelId, long movieId) {
        for (Movie movie : getMovies(context, channelId)) {
            if (movie.getId() == movieId) {
                return movie;
            }
        }
        return null;
    }
}
