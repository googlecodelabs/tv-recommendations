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

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.PersistableBundle;
import android.support.media.tv.Channel;
import android.support.media.tv.ChannelLogoUtils;
import android.support.media.tv.TvContractCompat;
import android.util.Log;
import com.example.android.tv.recommendations.RecommendationChannelJobService;
import com.example.android.tv.recommendations.RecommendationProgramsJobService;
import com.example.android.tv.recommendations.model.Subscription;

/** Manages interactions with the TV provider. */
public class TvUtil {

    private static final String TAG = "TvUtil";

    private static int mJobId = 0;

    public static long createChannel(Context context, Subscription subscription) {

        Uri appLinkIntentUri = Uri.parse(subscription.getAppLinkIntentUri());

        Channel.Builder builder = new Channel.Builder();
        builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(subscription.getName())
                .setDescription(subscription.getDescription())
                //TODO: follow up with isaackatz@. I thought this was not required anymore
                .setInputId("input_id")
                .setAppLinkIntentUri(appLinkIntentUri);

        Log.d(TAG, "Creating channel: " + subscription.getName());
        Uri channelUrl =
                context.getContentResolver()
                        .insert(
                                TvContractCompat.Channels.CONTENT_URI,
                                builder.build().toContentValues());

        Log.d(TAG, "channel insert at " + channelUrl);
        long channelId = ContentUris.parseId(channelUrl);
        Log.d(TAG, "channel id " + channelId);

        ChannelLogoUtils.storeChannelLogo(
                context,
                channelId,
                BitmapFactory.decodeResource(
                        context.getResources(), subscription.getChannelLogo()));

        return channelId;
    }

    public static void scheduleSyncingChannel(Context context) {
        ComponentName componentName =
                new ComponentName(context, RecommendationChannelJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(1, componentName);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        Log.d(TAG, "Scheduled channel creation.");
        scheduler.schedule(builder.build());
    }

    public static void scheduleSyncingProgramsForChannel(Context context, long channelId) {
        ComponentName componentName =
                new ComponentName(context, RecommendationProgramsJobService.class);

        JobInfo.Builder builder = new JobInfo.Builder(mJobId++, componentName);

        JobInfo.TriggerContentUri triggerContentUri =
                new JobInfo.TriggerContentUri(
                        TvContractCompat.Channels.CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS);
        builder.addTriggerContentUri(triggerContentUri);
        builder.setTriggerContentMaxDelay(0L);
        builder.setTriggerContentUpdateDelay(0L);

        PersistableBundle bundle = new PersistableBundle();
        bundle.putLong(TvContractCompat.EXTRA_CHANNEL_ID, channelId);
        builder.setExtras(bundle);

        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        scheduler.schedule(builder.build());
    }
}
