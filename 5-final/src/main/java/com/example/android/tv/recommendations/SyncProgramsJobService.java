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
package com.example.android.tv.recommendations;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.media.tv.Channel;
import android.support.media.tv.PreviewProgram;
import android.support.media.tv.TvContractCompat;
import android.util.Log;
import com.example.android.tv.recommendations.model.MockDatabase;
import com.example.android.tv.recommendations.model.MockMovieService;
import com.example.android.tv.recommendations.model.Movie;
import com.example.android.tv.recommendations.model.Subscription;
import com.example.android.tv.recommendations.util.TvUtil;
import java.util.ArrayList;
import java.util.List;

/**
 * A job service that will create a default channel for an application. Once a default channel is
 * created, it will pre-fill in some programs.
 */
public class SyncProgramsJobService extends JobService {

    private static final String TAG = "RecommendProgramJobSvc";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Starting program sync job.");

        long channelId = getChannelId(jobParameters);
        if (channelId == -1L) {
            return false;
        }
        Log.d(TAG, "onStartJob(): Scheduling syncing for programs for channel " + channelId);

        syncPrograms(channelId);

        // Daisy chain listening for the next change.
        TvUtil.scheduleSyncingProgramsForChannel(this, channelId);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private long getChannelId(JobParameters jobParameters) {
        PersistableBundle extras = jobParameters.getExtras();
        if (extras == null) {
            return -1L;
        }

        return extras.getLong(TvContractCompat.EXTRA_CHANNEL_ID, -1L);
    }

    private void syncPrograms(long channelId) {
        Log.d(TAG, "Sync programs for channel: " + channelId);

        try (Cursor cursor =
                getContentResolver()
                        .query(
                                TvContractCompat.buildChannelUri(channelId),
                                null,
                                null,
                                null,
                                null)) {
            if (cursor != null && cursor.moveToNext()) {
                Channel channel = Channel.fromCursor(cursor);
                if (!channel.isBrowsable()) {
                    Log.d(TAG, "Channel is not visible: " + channelId);
                } else {
                    Log.d(TAG, "Channel is visible, syncing programs: " + channelId);
                    Subscription subscription =
                            MockDatabase.findSubscriptionByChannelId(
                                    getApplicationContext(), channelId);

                    if (subscription != null) {

                        List<Movie> programs =
                                MockDatabase.getMovies(getApplicationContext(), channelId);
                        if (programs.isEmpty()) {
                            programs = createPrograms(subscription, MockMovieService.getList());
                        } else {
                            programs = updatePrograms(subscription, programs);
                        }

                        MockDatabase.saveMovies(getApplicationContext(), channelId, programs);
                    }
                }
            }
        }
    }

    private List<Movie> createPrograms(Subscription subscription, List<Movie> movies) {

        List<Movie> moviesAdded = new ArrayList<>(movies.size());
        for (Movie movie : movies) {
            PreviewProgram previewProgram = buildProgram(subscription, movie);

            Uri programUri =
                    getContentResolver()
                            .insert(
                                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                                    previewProgram.toContentValues());
            long programId = ContentUris.parseId(programUri);
            Log.d(TAG, "Inserted new program: " + programId);
            movie.setProgramId(programId);
            moviesAdded.add(movie);
        }

        return moviesAdded;
    }

    private List<Movie> updatePrograms(Subscription channel, List<Movie> programs) {

        for (Movie movie : programs) {
            getContentResolver()
                    .delete(
                            TvContractCompat.buildPreviewProgramUri(movie.getProgramId()),
                            null,
                            null);
        }

        // By getting a fresh list, we should see a visible change in the home screen.
        return createPrograms(channel, MockMovieService.getFreshList());
    }

    @NonNull
    private PreviewProgram buildProgram(Subscription subscription, Movie movie) {
        Uri posterArtUri = Uri.parse(movie.getCardImageUrl());

        Uri appLinkUri =
                Uri.parse(
                        getString(
                                R.string.app_link_play,
                                getString(R.string.schema),
                                getString(R.string.host),
                                subscription.getChannelId(),
                                movie.getTitle()));

        String title = movie.getTitle();

        PreviewProgram.Builder builder = new PreviewProgram.Builder();
        builder.setChannelId(subscription.getChannelId())
                .setType(TvContractCompat.PreviewProgramColumns.TYPE_CLIP)
                .setTitle(title)
                .setDescription(movie.getDescription())
                .setPosterArtUri(posterArtUri)
                .setPreviewVideoUri(Uri.parse(movie.getVideoUrl()))
                .setIntentUri(appLinkUri);
        return builder.build();
    }
}
