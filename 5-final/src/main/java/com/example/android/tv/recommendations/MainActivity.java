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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.media.tv.TvContractCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.example.android.tv.recommendations.model.MockDatabase;
import com.example.android.tv.recommendations.model.Subscription;
import com.example.android.tv.recommendations.util.TvUtil;

/*
 * Displays subscriptions that can be added to the main launcher's channels.
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final int MAKE_BROWSABLE_REQUEST_CODE = 9001;

    private Button mTvSubscribeButton;
    private Button mVideoClipSubscribeButton;
    private Button mRandomSubscribeButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTvSubscribeButton = (Button) findViewById(R.id.subscribe_tv_button);
        mVideoClipSubscribeButton = (Button) findViewById(R.id.subscribe_video_button);
        mRandomSubscribeButton = (Button) findViewById(R.id.subscribe_random_button);

        final Subscription tvShowSubscription =
                MockDatabase.getTvShowSubscription(getApplicationContext());
        setupButtonState(mTvSubscribeButton, tvShowSubscription);

        final Subscription videoSubscription =
                MockDatabase.getVideoSubscription(getApplicationContext());
        setupButtonState(mVideoClipSubscribeButton, videoSubscription);

        final Subscription randomSubscription =
                MockDatabase.getRandomSubscription(getApplicationContext());
        setupButtonState(mRandomSubscribeButton, randomSubscription);

        TvUtil.scheduleSyncingChannel(this);
    }

    private void setupButtonState(Button button, final Subscription subscription) {
        boolean channelExists = subscription.getChannelId() > 0L;
        button.setEnabled(!channelExists);
        button.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Create the channel in the provider.
                        long channelId = TvUtil.createChannel(MainActivity.this, subscription);
                        // Ask the user if the provider should make the channel visible in the launcher.
                        promptUserToDisplayChannel(channelId);
                    }
                });
    }

    private void promptUserToDisplayChannel(long channelId) {
        Intent intent = new Intent(TvContractCompat.ACTION_REQUEST_CHANNEL_BROWSABLE);
        intent.putExtra(TvContractCompat.EXTRA_CHANNEL_ID, channelId);
        try {
            this.startActivityForResult(intent, MAKE_BROWSABLE_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Could not start activity: " + intent.getAction(), e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Toast.makeText(this, R.string.channel_added, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, R.string.channel_not_added, Toast.LENGTH_LONG).show();
        }
    }
}
