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
import android.support.media.tv.TvContractCompat;
import android.util.Log;
import com.example.android.tv.recommendations.model.MockDatabase;
import com.example.android.tv.recommendations.model.Subscription;
import com.example.android.tv.recommendations.util.TvUtil;
import java.util.List;

/**
 * A service that will populate the TV Provider with channels that every user should have. Once a
 * channel is created, it trigger another service to add programs.
 */
public class RecommendationChannelJobService extends JobService {

    private static final String TAG = "RecommendChannelJobSvc";

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "Starting channel creation job");
        setupChannels();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    private void setupChannels() {

        List<Subscription> subscriptions = MockDatabase.getSubscriptions(getApplicationContext());
        int numOfChannelsInTVProvider = TvUtil.getNumberOfChannels(getApplicationContext());
        // Checks if the default channels are added. Since a user can add more channels from your app
        // later, the number of channels in the provider can be greater than the number of default
        // channels.
        if (numOfChannelsInTVProvider >= subscriptions.size() && !subscriptions.isEmpty()) {
            Log.d(TAG, "Already loaded default channels into the provider");
        } else {
            // Create subscriptions from mocked source.
            subscriptions = MockDatabase.createUniversalSubscriptions(getApplicationContext());
            for (Subscription subscription : subscriptions) {
                long channelId = TvUtil.createChannel(getApplicationContext(), subscription);
                subscription.setChannelId(channelId);
            }

            // You only get one channel that can be visible by default. The other subscriptions'
            // visibility will be controlled by the user.
            Subscription flagshipChannel = subscriptions.get(0);
            TvContractCompat.requestChannelBrowsable(
                    getApplicationContext(), flagshipChannel.getChannelId());

            MockDatabase.saveSubscriptions(getApplicationContext(), subscriptions);
        }

        // Kick off a job to update default programs.
        // The program job should verify if the channel is visible before updating programs.
        for (Subscription channel : subscriptions) {
            TvUtil.scheduleSyncingProgramsForChannel(
                    getApplicationContext(), channel.getChannelId());
        }
    }
}
