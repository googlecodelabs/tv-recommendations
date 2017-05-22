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
import android.content.Context;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.media.tv.PreviewProgram;
import android.support.media.tv.TvContractCompat;
import android.util.Log;
import com.example.android.tv.recommendations.model.MockDatabase;
import com.example.android.tv.recommendations.model.Movie;
import com.example.android.tv.recommendations.model.Subscription;
import com.example.android.tv.recommendations.util.TvUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Syncs programs for a channel. A channel id is required to be passed via the {@link
 * JobParameters}. This service is scheduled to listen to changes to a channel. Once the job
 * completes, it will reschedule itself to listen for the next change to the channel. See {@link
 * TvUtil#scheduleSyncingProgramsForChannel(Context, long)} for more details about the scheduling.
 */
public class SyncProgramsJobService extends JobService {

    private static final String TAG = "SyncProgramsJobService";

    private SyncProgramsTask mSyncProgramsTask;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.d(TAG, "onStartJob(): " + jobParameters);

        final long channelId = getChannelId(jobParameters);
        if (channelId == -1L) {
            return false;
        }
        Log.d(TAG, "onStartJob(): Scheduling syncing for programs for channel " + channelId);

        mSyncProgramsTask =
                new SyncProgramsTask(getApplicationContext()) {
                    @Override
                    protected void onPostExecute(Boolean finished) {
                        super.onPostExecute(finished);
                        // Daisy chain listening for the next change to the channel.
                        TvUtil.scheduleSyncingProgramsForChannel(
                                SyncProgramsJobService.this, channelId);
                        mSyncProgramsTask = null;
                        jobFinished(jobParameters, !finished);
                    }
                };
        mSyncProgramsTask.execute(channelId);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mSyncProgramsTask != null) {
            mSyncProgramsTask.cancel(true);
        }
        return true;
    }

    private long getChannelId(JobParameters jobParameters) {
        PersistableBundle extras = jobParameters.getExtras();
        if (extras == null) {
            return -1L;
        }

        return extras.getLong(TvContractCompat.EXTRA_CHANNEL_ID, -1L);
    }

    /*
     * Syncs programs by querying the given channel id.
     *
     * If the channel is not browsable, the programs will be removed to avoid showing
     * stale programs when the channel becomes browsable in the future.
     *
     * If the channel is browsable, then it will check if the channel has any programs.
     *      If the channel does not have any programs, new programs will be added.
     *      If the channel does have programs, then a fresh list of programs will be fetched and the
     *          channel's programs will be updated.
     */
    private void syncPrograms(long channelId, List<Movie> initialMovies) {
        Log.d(TAG, "Sync programs for channel: " + channelId);
        List<Movie> movies = new ArrayList<>(initialMovies);

        // TODO: step 5 check if visible.

    }

    private List<Movie> createPrograms(long channelId, List<Movie> movies) {
        // TODO: step 7 add programs.
        return Collections.emptyList();
    }

    private List<Movie> updatePrograms(long channelId, List<Movie> movies) {
        // TODO: step 8 update programs.
        return Collections.emptyList();
    }

    private void deletePrograms(long channelId, List<Movie> movies) {
        if (movies.isEmpty()) {
            return;
        }

        // TODO: step 9 delete programs.

        // Remove our local records to stay in sync with the TV Provider.
        MockDatabase.removeMovies(getApplicationContext(), channelId);
    }

    @NonNull
    private PreviewProgram buildProgram(long channelId, Movie movie) {
        // TODO: step 6 convert movie to program
        return null;
    }

    private class SyncProgramsTask extends AsyncTask<Long, Void, Boolean> {

        private final Context mContext;

        private SyncProgramsTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected Boolean doInBackground(Long... channelIds) {
            List<Long> params = Arrays.asList(channelIds);
            if (!params.isEmpty()) {
                for (Long channelId : params) {
                    Subscription subscription =
                            MockDatabase.findSubscriptionByChannelId(mContext, channelId);
                    if (subscription != null) {
                        List<Movie> cachedMovies = MockDatabase.getMovies(mContext, channelId);
                        syncPrograms(channelId, cachedMovies);
                    }
                }
            }
            return true;
        }
    }
}
