/*
 * Copyright (c) 2017 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.example.android.tv.recommendations.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.example.android.tv.recommendations.model.Movie;
import com.example.android.tv.recommendations.model.Subscription;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to store {@link Subscription}s and {@link Movie}s in {@link SharedPreferences}.
 *
 * <p>SharedPreferencesHelper provides static methods to set and get these objects.
 *
 * <p>The methods of this class should not be called on the UI thread.
 */
public final class SharedPreferencesHelper {

    private static final String TAG = "SharedPreferencesHelper";

    private static final String PREFS_NAME = "com.example.android.tv.recommendations";
    private static final String PREFS_SUBSCRIPTIONS_KEY =
            "com.example.android.tv.recommendations.prefs.SUBSCRIPTIONS";
    private static final String PREFS_MEDIA_CONTENT_PREFIX =
            "com.example.android.tv.recommendations.prefs.MEDIA_CONTENT_";

    private static final Gson mGson = new Gson();

    /**
     * Reads the {@link List<Subscription>} from {@link SharedPreferences}.
     *
     * @param context used for getting an instance of shared preferences
     * @return a list of subscriptions or an empty list if none exist
     * @throws JsonSyntaxException if subscriptions cannot be read
     */
    public static List<Subscription> readSubscriptions(Context context) {
        return getList(context, Subscription.class, PREFS_SUBSCRIPTIONS_KEY);
    }

    /**
     * Overrides the subscriptions stored in {@link SharedPreferences}.
     *
     * @param context used for getting an instance of shared preferences
     * @param subscriptions to be stored in shared preferences
     */
    public static void storeSubscriptions(Context context, List<Subscription> subscriptions) {
        setList(context, subscriptions, PREFS_SUBSCRIPTIONS_KEY);
    }

    /**
     * Reads the {@link List<Movie>} from {@link SharedPreferences} for a given channel.
     *
     * @param context used for getting an instance of shared preferences
     * @param channelId of the channel that the movies are associated with
     * @return a list of movies or an empty list if none exist
     * @throws JsonSyntaxException if movies cannot be read
     */
    public static List<Movie> readMovies(Context context, long channelId) {
        return getList(context, Movie.class, PREFS_MEDIA_CONTENT_PREFIX + channelId);
    }

    /**
     * Overrides the movies stored in {@link SharedPreferences} for the associated channelId.
     *
     * @param context used for getting an instance of shared preferences
     * @param channelId of the channel that the movies are associated with
     * @param movies to be stored
     */
    public static void storeMovies(Context context, long channelId, List<Movie> movies) {
        setList(context, movies, PREFS_MEDIA_CONTENT_PREFIX + channelId);
    }

    /**
     * A helper method to manage getting a string set from shared preferences and converting the
     * strings in the set into objects.
     *
     * @param context used for getting an instance of shared preferences
     * @param clazz the class that the strings will be unmarshalled into
     * @param key the key in shared preferences to access the string set
     * @param <T> the type of object that will be in the returned list, should be the same as the
     *     clazz that was supplied
     * @return a list of <T> objects that were stored in shared preferences or an empty list if no
     *     objects exists
     */
    private static <T> List<T> getList(Context context, Class<T> clazz, String key) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> contactsSet = sharedPreferences.getStringSet(key, new HashSet<String>());
        if (contactsSet.isEmpty()) {
            // Favoring mutability of the list over Collections.emptyList().
            return new ArrayList<>();
        }
        List<T> list = new ArrayList<>(contactsSet.size());
        try {
            for (String contactString : contactsSet) {
                list.add(mGson.fromJson(contactString, clazz));
            }
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Could not parse json.", e);
            return Collections.emptyList();
        }
        return list;
    }

    /**
     * A helper method to set a set of strings in shared preferences. This will loop through the
     * list and write each object as a string and persist the set of strings in shared preferences.
     *
     * @param context used for getting an instance of shared preferences
     * @param list of <T> object that need to be persisted
     * @param key the key in shared preferences which the string set will be stored
     * @param <T> type the of object we will be marshalling and persisting
     */
    private static <T> void setList(Context context, List<T> list, String key) {
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        Set<String> strings = new LinkedHashSet<>(list.size());
        for (T item : list) {
            strings.add(mGson.toJson(item));
        }
        editor.putStringSet(key, strings);
        editor.apply();
    }
}
