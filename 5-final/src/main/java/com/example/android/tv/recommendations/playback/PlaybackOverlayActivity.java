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

package com.example.android.tv.recommendations.playback;

import android.app.Activity;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.view.KeyEvent;
import android.widget.VideoView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.android.tv.recommendations.R;
import com.example.android.tv.recommendations.model.Movie;

/** PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment. */
public class PlaybackOverlayActivity extends Activity
        implements PlaybackOverlayFragment.OnPlayPauseClickedListener {
    private static final String TAG = "PlaybackOverlayActivity";

    public static final String MOVIE = "Movie";

    @IntDef({STATE_PLAYING, STATE_PAUSED, STATE_BUFFERING, STATE_IDLE})
    private @interface LeanbackPlaybackState {}

    private static final int STATE_PLAYING = 20;
    private static final int STATE_PAUSED = 21;
    private static final int STATE_BUFFERING = 22;
    private static final int STATE_IDLE = 23;

    private VideoView mVideoView;
    private @LeanbackPlaybackState int mPlaybackState = STATE_IDLE;
    private MediaSession mSession;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playback_controls);
        loadViews();
        setupCallbacks();
        mSession = new MediaSession(this, "LeanbackSampleApp");
        mSession.setCallback(new NoOptMediaSessionCallback());
        mSession.setFlags(
                MediaSession.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mSession.setActive(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSession.setActive(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayback();
        mSession.release();
        mVideoView.suspend();
    }

    private void stopPlayback() {
        if (mVideoView != null && mVideoView.isPlaying()) {
            if (!requestVisibleBehind(true)) {
                // Try to play behind launcher, but if it fails, stop playback.
                mVideoView.stopPlayback();
            }
        } else {
            requestVisibleBehind(false);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        PlaybackOverlayFragment playbackOverlayFragment =
                (PlaybackOverlayFragment)
                        getFragmentManager().findFragmentById(R.id.playback_controls_fragment);
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                playbackOverlayFragment.togglePlayback(false);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                playbackOverlayFragment.togglePlayback(false);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (mPlaybackState == STATE_PLAYING) {
                    playbackOverlayFragment.togglePlayback(false);
                } else {
                    playbackOverlayFragment.togglePlayback(true);
                }
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    /** Implementation of OnPlayPauseClickedListener. */
    public void onFragmentPlayPause(Movie movie, int position, Boolean playPause) {
        mVideoView.setVideoPath(movie.getVideoUrl());

        if (position == 0 || mPlaybackState == STATE_IDLE) {
            setupCallbacks();
            mPlaybackState = STATE_IDLE;
        }

        if (playPause && mPlaybackState != STATE_PLAYING) {
            mPlaybackState = STATE_PLAYING;
            if (position > 0) {
                mVideoView.seekTo(position);
                mVideoView.start();
            }
        } else {
            mPlaybackState = STATE_PAUSED;
            mVideoView.pause();
        }
        updatePlaybackState(position);
        updateMetadata(movie);
    }

    private void updatePlaybackState(int position) {
        PlaybackState.Builder stateBuilder =
                new PlaybackState.Builder().setActions(getAvailableActions());
        int state = PlaybackState.STATE_PLAYING;
        if (mPlaybackState == STATE_PAUSED) {
            state = PlaybackState.STATE_PAUSED;
        }
        stateBuilder.setState(state, position, 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    private long getAvailableActions() {
        long actions =
                PlaybackState.ACTION_PLAY
                        | PlaybackState.ACTION_PLAY_FROM_MEDIA_ID
                        | PlaybackState.ACTION_PLAY_FROM_SEARCH;

        if (mPlaybackState == STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }

        return actions;
    }

    private void updateMetadata(final Movie movie) {
        final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        String title = movie.getTitle().replace("_", " -");

        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title);
        metadataBuilder.putString(
                MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, movie.getDescription());
        metadataBuilder.putString(
                MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, movie.getCardImageUrl());

        // At a minimum, we add the title and artist for legacy support.
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, title);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, movie.getStudio());

        Glide.with(this)
                .load(Uri.parse(movie.getCardImageUrl()))
                .asBitmap()
                .into(
                        new SimpleTarget<Bitmap>(500, 500) {
                            @Override
                            public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                                mSession.setMetadata(metadataBuilder.build());
                            }
                        });
    }

    private void loadViews() {
        mVideoView = (VideoView) findViewById(R.id.video_view);
        mVideoView.setFocusable(false);
        mVideoView.setFocusableInTouchMode(false);
    }

    private void setupCallbacks() {

        mVideoView.setOnErrorListener(
                new MediaPlayer.OnErrorListener() {

                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        String msg = "";
                        if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                            msg = getString(R.string.video_error_media_load_timeout);
                        } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                            msg = getString(R.string.video_error_server_inaccessible);
                        } else {
                            msg = getString(R.string.video_error_unknown_error);
                        }
                        mVideoView.stopPlayback();
                        mPlaybackState = STATE_IDLE;
                        return false;
                    }
                });

        mVideoView.setOnPreparedListener(
                new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        if (mPlaybackState == STATE_PLAYING) {
                            mVideoView.start();
                        }
                    }
                });

        mVideoView.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mPlaybackState = STATE_IDLE;
                    }
                });
    }

    @Override
    public void onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled();
    }

    /** {@link android.media.session.MediaSession.Callback} is required by MediaSession. */
    private class NoOptMediaSessionCallback extends MediaSession.Callback {}
}
