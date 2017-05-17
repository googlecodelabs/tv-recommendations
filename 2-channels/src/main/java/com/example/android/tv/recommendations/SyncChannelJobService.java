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
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.media.tv.Channel;
import android.support.media.tv.ChannelLogoUtils;
import android.support.media.tv.TvContractCompat;
import android.util.Log;
import com.example.android.tv.recommendations.model.MockDatabase;
import com.example.android.tv.recommendations.model.MockMovieService;
import com.example.android.tv.recommendations.model.Subscription;
import com.example.android.tv.recommendations.util.TvUtil;
import java.util.List;

/**
 * A service that will populate the TV provider with channels that every user should have. Once a
 * channel is created, it trigger another service to add programs.
 */
public class SyncChannelJobService extends JobService {

    private static final String TAG = "RecommendChannelJobSvc";

    private SyncChannelTask mSyncChannelTask;

    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        Log.d(TAG, "Starting channel creation job");
        mSyncChannelTask =
                new SyncChannelTask(getApplicationContext()) {
                    @Override
                    protected void onPostExecute(Boolean success) {
                        super.onPostExecute(success);
                        jobFinished(jobParameters, !success);
                    }
                };
        mSyncChannelTask.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        if (mSyncChannelTask != null) {
            mSyncChannelTask.cancel(true);
        }
        return true;
    }

    private static class SyncChannelTask extends AsyncTask<Void, Void, Boolean> {

        private final Context mContext;

        SyncChannelTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            List<Subscription> subscriptions = MockDatabase.getSubscriptions(mContext);
            int numOfChannelsInTVProvider = TvUtil.getNumberOfChannels(mContext);
            // Checks if the default channels are added. Since a user can add more channels from
            // your app later, the number of channels in the provider can be greater than the number
            // of default channels.
            if (numOfChannelsInTVProvider >= subscriptions.size() && !subscriptions.isEmpty()) {
                Log.d(TAG, "Already loaded default channels into the provider");
            } else {
                // Create subscriptions from mocked source.
                subscriptions = MockMovieService.createUniversalSubscriptions(mContext);
                for (Subscription subscription : subscriptions) {
                    long channelId = createChannel(mContext, subscription);
                    subscription.setChannelId(channelId);
                    // TODO: step 3 make the channel visible
                    TvContractCompat.requestChannelBrowsable(mContext, channelId);
                }

                MockDatabase.saveSubscriptions(mContext, subscriptions);
            }

            // Kick off a job to update default programs.
            // The program job should verify if the channel is visible before updating programs.
            for (Subscription channel : subscriptions) {
                TvUtil.scheduleSyncingProgramsForChannel(mContext, channel.getChannelId());
            }
            return true;
        }

        private long createChannel(Context context, Subscription subscription) {
            // TODO: step 2 create a channel
            // Checks if our subscription has been added to the channels before.
            long channelId = getChannelIdFromTvProvider(context, subscription);
            if (channelId != -1L) {
                return channelId;
            }

            // Create the channel since it has not been added to the TV Provider.
            Uri appLinkIntentUri = Uri.parse(subscription.getAppLinkIntentUri());

            Channel.Builder builder = new Channel.Builder();
            builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                    .setDisplayName(subscription.getName())
                    .setDescription(subscription.getDescription())
                    .setAppLinkIntentUri(appLinkIntentUri);

            Log.d(TAG, "Creating channel: " + subscription.getName());
            Uri channelUrl =
                    context.getContentResolver()
                            .insert(
                                    TvContractCompat.Channels.CONTENT_URI,
                                    builder.build().toContentValues());

            Log.d(TAG, "channel insert at " + channelUrl);
            channelId = ContentUris.parseId(channelUrl);
            Log.d(TAG, "channel id " + channelId);

            Bitmap bitmap = TvUtil.convertToBitmap(context, subscription.getChannelLogo());
            ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap);

            return channelId;
        }

        private long getChannelIdFromTvProvider(Context context, Subscription subscription) {
            // TODO: step 1 query for channel
            Cursor cursor =
                    context.getContentResolver()
                            .query(
                                    TvContractCompat.Channels.CONTENT_URI,
                                    new String[] {
                                        TvContractCompat.Channels._ID,
                                        TvContract.Channels.COLUMN_DISPLAY_NAME
                                    },
                                    null,
                                    null,
                                    null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Channel channel = Channel.fromCursor(cursor);
                    if (subscription.getName().equals(channel.getDisplayName())) {
                        Log.d(
                                TAG,
                                "Channel already exists. Returning channel "
                                        + channel.getId()
                                        + " from TV Provider.");
                        return channel.getId();
                    }
                } while (cursor.moveToNext());
            }
            return -1L;
        }
    }
}
