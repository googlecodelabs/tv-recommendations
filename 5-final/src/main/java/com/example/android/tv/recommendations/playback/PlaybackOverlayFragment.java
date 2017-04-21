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

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ShuffleAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.util.Log;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.android.tv.recommendations.R;
import com.example.android.tv.recommendations.model.MockMovieService;
import com.example.android.tv.recommendations.model.Movie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * Handles video playback with media controls.
 */
public class PlaybackOverlayFragment
        extends android.support.v17.leanback.app.PlaybackOverlayFragment {
    private static final String TAG = "PlaybackControlsFragmnt";

    private static final boolean HIDE_MORE_ACTIONS = false;
    private static final int PRIMARY_CONTROLS = 5;
    private static final boolean SHOW_IMAGE = PRIMARY_CONTROLS <= 5;
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_LIGHT;
    private static final int CARD_WIDTH = 200;
    private static final int CARD_HEIGHT = 240;
    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int UPDATE_PERIOD = 16;
    private static final int SIMULATED_BUFFERED_TIME = 10000;

    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private PlayPauseAction mPlayPauseAction;
    private RepeatAction mRepeatAction;
    private ThumbsUpAction mThumbsUpAction;
    private ThumbsDownAction mThumbsDownAction;
    private ShuffleAction mShuffleAction;
    private FastForwardAction mFastForwardAction;
    private RewindAction mRewindAction;
    private SkipNextAction mSkipNextAction;
    private SkipPreviousAction mSkipPreviousAction;
    private PlaybackControlsRow mPlaybackControlsRow;
    private ArrayList<Movie> mItems = new ArrayList<>();
    private int mCurrentItem;
    private Handler mHandler;
    private Runnable mRunnable;
    private Movie mSelectedMovie;

    private OnPlayPauseClickedListener mCallback;

    /** Listener for play/pause state. */
    public interface OnPlayPauseClickedListener {
        void onFragmentPlayPause(Movie movie, int position, Boolean playPause);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mItems = new ArrayList<>();
        mSelectedMovie =
                (Movie)
                        getActivity()
                                .getIntent()
                                .getSerializableExtra(PlaybackOverlayActivity.MOVIE);

        List<Movie> movies = MockMovieService.getList();

        for (int i = 0; i < movies.size(); i++) {
            mItems.add(movies.get(i));
            if (mSelectedMovie.getTitle().contentEquals(movies.get(i).getTitle())) {
                mCurrentItem = i;
            }
        }

        mHandler = new Handler();

        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);

        setupRows();

        setOnItemViewSelectedListener(
                new OnItemViewSelectedListener() {
                    @Override
                    public void onItemSelected(
                            Presenter.ViewHolder itemViewHolder,
                            Object item,
                            RowPresenter.ViewHolder rowViewHolder,
                            Row row) {
                        Log.v(TAG, "onItemSelected: " + item + " row " + row);
                    }
                });
        setOnItemViewClickedListener(
                new OnItemViewClickedListener() {
                    @Override
                    public void onItemClicked(
                            Presenter.ViewHolder itemViewHolder,
                            Object item,
                            RowPresenter.ViewHolder rowViewHolder,
                            Row row) {
                        Log.v(TAG, "onItemClicked: " + item + " row " + row);
                    }
                });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnPlayPauseClickedListener) {
            mCallback = (OnPlayPauseClickedListener) context;
        } else {
            throw new IllegalArgumentException(
                    context.getClass().getCanonicalName()
                            + " must implement OnPlayPauseClickedListener");
        }
    }

    private void setupRows() {

        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();

        PlaybackControlsRowPresenter playbackControlsRowPresenter =
                new PlaybackControlsRowPresenter(new DescriptionPresenter());
        playbackControlsRowPresenter.setOnActionClickedListener(
                new OnActionClickedListener() {
                    public void onActionClicked(Action action) {
                        if (action.getId() == mPlayPauseAction.getId()) {
                            togglePlayback(mPlayPauseAction.getIndex() == PlayPauseAction.PLAY);
                        } else if (action.getId() == mSkipNextAction.getId()) {
                            next();
                        } else if (action.getId() == mSkipPreviousAction.getId()) {
                            prev();
                        } else if (action.getId() == mFastForwardAction.getId()) {
                            // TODO: Implement fast forwarding.
                            Toast.makeText(getActivity(), R.string.fast_forward, Toast.LENGTH_SHORT)
                                    .show();
                        } else if (action.getId() == mRewindAction.getId()) {
                            // TODO: Implement rewinding.
                            Toast.makeText(getActivity(), R.string.rewind, Toast.LENGTH_SHORT)
                                    .show();
                        }
                        if (action instanceof PlaybackControlsRow.MultiAction) {
                            ((PlaybackControlsRow.MultiAction) action).nextIndex();
                            notifyChanged(action);
                        }
                    }
                });
        playbackControlsRowPresenter.setSecondaryActionsHidden(HIDE_MORE_ACTIONS);

        presenterSelector.addClassPresenter(
                PlaybackControlsRow.class, playbackControlsRowPresenter);
        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(presenterSelector);

        addPlaybackControlsRow();

        setAdapter(mRowsAdapter);
    }

    public void togglePlayback(boolean playPause) {
        if (playPause) {
            startProgressAnimation();
            setFadingEnabled(true);
            mCallback.onFragmentPlayPause(
                    mItems.get(mCurrentItem), mPlaybackControlsRow.getCurrentTime(), true);
            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlayPauseAction.PAUSE));
        } else {
            stopProgressAnimation();
            setFadingEnabled(false);
            mCallback.onFragmentPlayPause(
                    mItems.get(mCurrentItem), mPlaybackControlsRow.getCurrentTime(), false);
            mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlayPauseAction.PLAY));
        }
        notifyChanged(mPlayPauseAction);
    }

    private int getDuration() {
        Movie movie = mItems.get(mCurrentItem);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mmr.setDataSource(movie.getVideoUrl(), new HashMap<String, String>());
        } else {
            mmr.setDataSource(movie.getVideoUrl());
        }
        String time = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long duration = Long.parseLong(time);
        return (int) duration;
    }

    private void addPlaybackControlsRow() {
        mPlaybackControlsRow = new PlaybackControlsRow();
        mRowsAdapter.add(mPlaybackControlsRow);

        updatePlaybackRow(mCurrentItem);

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mSecondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);

        mPlayPauseAction = new PlayPauseAction(getActivity());
        mRepeatAction = new RepeatAction(getActivity());
        mThumbsUpAction = new ThumbsUpAction(getActivity());
        mThumbsDownAction = new ThumbsDownAction(getActivity());
        mShuffleAction = new ShuffleAction(getActivity());
        mSkipNextAction = new SkipNextAction(getActivity());
        mSkipPreviousAction = new SkipPreviousAction(getActivity());
        mFastForwardAction = new FastForwardAction(getActivity());
        mRewindAction = new RewindAction(getActivity());

        if (PRIMARY_CONTROLS > 5) {
            mPrimaryActionsAdapter.add(mThumbsUpAction);
        } else {
            mSecondaryActionsAdapter.add(mThumbsUpAction);
        }
        mPrimaryActionsAdapter.add(mSkipPreviousAction);
        if (PRIMARY_CONTROLS > 3) {
            mPrimaryActionsAdapter.add(new RewindAction(getActivity()));
        }
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        if (PRIMARY_CONTROLS > 3) {
            mPrimaryActionsAdapter.add(new FastForwardAction(getActivity()));
        }
        mPrimaryActionsAdapter.add(mSkipNextAction);

        mSecondaryActionsAdapter.add(mRepeatAction);
        mSecondaryActionsAdapter.add(mShuffleAction);
        if (PRIMARY_CONTROLS > 5) {
            mPrimaryActionsAdapter.add(mThumbsDownAction);
        } else {
            mSecondaryActionsAdapter.add(mThumbsDownAction);
        }
        mSecondaryActionsAdapter.add(new PlaybackControlsRow.HighQualityAction(getActivity()));
        mSecondaryActionsAdapter.add(new PlaybackControlsRow.ClosedCaptioningAction(getActivity()));
    }

    private void notifyChanged(Action action) {
        if (mPrimaryActionsAdapter.indexOf(action) >= 0) {
            mPrimaryActionsAdapter.notifyArrayItemRangeChanged(
                    mPrimaryActionsAdapter.indexOf(action), 1);
            return;
        }
        if (mSecondaryActionsAdapter.indexOf(action) >= 0) {
            mSecondaryActionsAdapter.notifyArrayItemRangeChanged(
                    mSecondaryActionsAdapter.indexOf(action), 1);
            return;
        }
    }

    private void updatePlaybackRow(int index) {
        if (mPlaybackControlsRow.getItem() != null) {
            Movie item = (Movie) mPlaybackControlsRow.getItem();
            item.setTitle(mItems.get(mCurrentItem).getTitle());
            item.setStudio(mItems.get(mCurrentItem).getStudio());
        }
        if (SHOW_IMAGE) {
            updateVideoImage(mItems.get(mCurrentItem).getCardImageUrl());
        }
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
        mPlaybackControlsRow.setTotalTime(getDuration());
        mPlaybackControlsRow.setCurrentTime(0);
        mPlaybackControlsRow.setBufferedProgress(0);
    }

    private int getUpdatePeriod() {
        if (getView() == null || mPlaybackControlsRow.getTotalTime() <= 0) {
            return DEFAULT_UPDATE_PERIOD;
        }
        return Math.max(UPDATE_PERIOD, mPlaybackControlsRow.getTotalTime() / getView().getWidth());
    }

    private void startProgressAnimation() {
        mRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        int updatePeriod = getUpdatePeriod();
                        int currentTime = mPlaybackControlsRow.getCurrentTime() + updatePeriod;
                        int totalTime = mPlaybackControlsRow.getTotalTime();
                        mPlaybackControlsRow.setCurrentTime(currentTime);
                        mPlaybackControlsRow.setBufferedProgress(
                                currentTime + SIMULATED_BUFFERED_TIME);

                        if (totalTime > 0 && totalTime <= currentTime) {
                            next();
                        }
                        mHandler.postDelayed(this, updatePeriod);
                    }
                };
        mHandler.postDelayed(mRunnable, getUpdatePeriod());
    }

    private void next() {
        if (++mCurrentItem >= mItems.size()) {
            mCurrentItem = 0;
        }

        if (mPlayPauseAction.getIndex() == PlayPauseAction.PLAY) {
            mCallback.onFragmentPlayPause(mItems.get(mCurrentItem), 0, false);
        } else {
            mCallback.onFragmentPlayPause(mItems.get(mCurrentItem), 0, true);
        }
        updatePlaybackRow(mCurrentItem);
    }

    private void prev() {
        if (--mCurrentItem < 0) {
            mCurrentItem = mItems.size() - 1;
        }
        if (mPlayPauseAction.getIndex() == PlayPauseAction.PLAY) {
            mCallback.onFragmentPlayPause(mItems.get(mCurrentItem), 0, false);
        } else {
            mCallback.onFragmentPlayPause(mItems.get(mCurrentItem), 0, true);
        }
        updatePlaybackRow(mCurrentItem);
    }

    private void stopProgressAnimation() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    @Override
    public void onStop() {
        stopProgressAnimation();
        super.onStop();
    }

    protected void updateVideoImage(String uri) {
        Glide.with(getActivity())
                .load(uri)
                .centerCrop()
                .into(
                        new SimpleTarget<GlideDrawable>(CARD_WIDTH, CARD_HEIGHT) {
                            @Override
                            public void onResourceReady(
                                    GlideDrawable resource,
                                    GlideAnimation<? super GlideDrawable> glideAnimation) {
                                mPlaybackControlsRow.setImageDrawable(resource);
                                mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
                            }
                        });
    }

    static class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            viewHolder.getTitle().setText(((Movie) item).getTitle());
            viewHolder.getSubtitle().setText(((Movie) item).getStudio());
        }
    }
}
