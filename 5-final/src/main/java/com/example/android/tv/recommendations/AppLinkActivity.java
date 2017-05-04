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
package com.example.android.tv.recommendations;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;
import com.example.android.tv.recommendations.model.MockDatabase;
import com.example.android.tv.recommendations.model.Movie;
import com.example.android.tv.recommendations.model.Subscription;
import com.example.android.tv.recommendations.playback.PlaybackActivity;
import java.util.List;

/**
 * Delegates to the correct activity based on how the user entered the app.
 *
 * <p>Supports two options: view and play. The view option will open the channel for the user to be
 * able to view more programs. The play option will load the channel/program,
 * subscriptions/mediaContent start playing the movie.
 */
public class AppLinkActivity extends Activity {

    private static final String TAG = "AppLinkActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        Uri data = intent.getData();

        Log.d(TAG, data.toString());

        List<String> segments = data.getPathSegments();
        if (segments.isEmpty()) {
            Log.e(TAG, "Invalid data " + data);
            finish();
            return;
        }

        String option = segments.get(0);
        if (option.equals(getString(R.string.play)) && segments.size() == 3) {
            long channelId = Long.parseLong(segments.get(1));
            String movieTitle = segments.get(2);
            Log.d(
                    TAG,
                    String.format("Playing program '%s' from channel %d", movieTitle, channelId));

            Movie movie =
                    MockDatabase.findMovieByTitle(getApplicationContext(), channelId, movieTitle);
            if (movie == null) {
                Log.e(TAG, "Invalid program " + data);
                finish();
                return;
            }

            Intent playMovieIntent = new Intent(this, PlaybackActivity.class);
            playMovieIntent.putExtra(PlaybackActivity.EXTRA_MOVIE, movie);
            playMovieIntent.putExtra(PlaybackActivity.EXTRA_CHANNEL_ID, channelId);
            startActivity(playMovieIntent);
        } else if (option.equals(getString(R.string.view)) && segments.size() == 2) {
            String name = segments.get(1);

            Subscription subscription =
                    MockDatabase.findSubscriptionByName(getApplicationContext(), name);
            if (subscription == null) {
                Log.e(TAG, "Invalid channel " + data);
                finish();
                return;
            }
            //TODO: open up an activity that has the subscription data and movies
            Toast.makeText(this, subscription.getName(), Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
