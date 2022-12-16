/*
 * Copyright 2017 Phillip Hsu
 *
 * This file is part of ClockPlus.
 *
 * ClockPlus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ClockPlus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ClockPlus.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.philliphsu.clock2.stopwatch.ui;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ContentInfoCompat;
import androidx.loader.content.Loader;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.JsonElement;
import com.lwkandroid.imagepicker.data.ImageBean;
import com.lwkandroid.widget.ngv.DefaultNgvAdapter;
import com.lwkandroid.widget.ngv.NgvChildImageView;
import com.lwkandroid.widget.ngv.NineGridView;
import com.philliphsu.clock2.BaseFragment;
import com.philliphsu.clock2.R;
import com.philliphsu.clock2.list.RecyclerViewFragment;
import com.philliphsu.clock2.stopwatch.Lap;
import com.philliphsu.clock2.stopwatch.StopwatchNotificationService;
import com.philliphsu.clock2.stopwatch.data.LapCursor;
import com.philliphsu.clock2.stopwatch.data.LapsCursorLoader;
import com.philliphsu.clock2.util.GsonUtil;
import com.philliphsu.clock2.util.HttpUrl;
import com.philliphsu.clock2.util.OKHttp3Util;
import com.philliphsu.clock2.util.PeerResultVO;
import com.philliphsu.clock2.util.ProgressBarUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.Request;

/**
 * Created by Phillip Hsu on 8/8/2016.
 */
public class StopwatchFragment  extends BaseFragment {
    private static final String TAG = "StopwatchFragment";

    // Exposed for StopwatchNotificationService
    public static final String KEY_START_TIME          = "start_time";
    public static final String KEY_PAUSE_TIME          = "pause_time";
    public static final String KEY_CHRONOMETER_RUNNING = "chronometer_running";

    private SharedPreferences                   mPrefs;
    private WeakReference<FloatingActionButton> mActivityFab;
    private Drawable                            mStartDrawable;
    private Drawable                            mPauseDrawable;

    @BindView(R.id.chronometer) ChronometerWithMillis mChronometer;
    @BindView(R.id.new_lap)     ImageButton           mNewLapButton;
    @BindView(R.id.stop)        ImageButton           mStopButton;

    @BindView(R.id.ninegridview)
    NineGridView mNineGridView;

    private GlideDisplayer imageLoader;

    private DefaultNgvAdapter<ImageBean> mAdapter;

    private String ipStr;

    /**
     * This is called only when a new instance of this Fragment is being created,
     * especially if the user is navigating to this tab for the first time in
     * this app session.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        // TODO: Will these be kept alive after onDestroyView()? If not, we should move these to
        // onCreateView() or any other callback that is guaranteed to be called.
        mStartDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_start_24dp);
        mPauseDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.ic_pause_24dp);

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        View view = super.onCreateView(inflater, container, savedInstanceState);

        mChronometer.setShowCentiseconds(true, true);
        mPrefs.edit()
                .putLong(StopwatchFragment.KEY_START_TIME, 0)
                .putLong(StopwatchFragment.KEY_PAUSE_TIME, 0)
                .putBoolean(StopwatchFragment.KEY_CHRONOMETER_RUNNING, false)
                .apply();
        long startTime = getLongFromPref(KEY_START_TIME);
//        long startTime = 0;
        long pauseTime = getLongFromPref(KEY_PAUSE_TIME);
//        mChronometer.setBase(0);
        // If we have a nonzero startTime from a previous session, restore it as
        // the chronometer's base. Otherwise, leave the default base.
        if (startTime > 0) {
            if (pauseTime > 0) {
                startTime += SystemClock.elapsedRealtime() - pauseTime;
            }
            mChronometer.setBase(startTime);
        }
        if (isStopwatchRunning()) {
            mChronometer.start();
            // Note: mChronometer.isRunning() will return false at this point and
            // in other upcoming lifecycle methods because it is not yet visible
            // (i.e. mVisible == false).
        }
        // The primary reason we call this is to show the mini FABs after rotate,
        // if the stopwatch is running. If the stopwatch is stopped, then this
        // would have hidden the mini FABs, if not for us already setting its
        // visibility to invisible in XML. We haven't initialized the WeakReference to
        // our Activity's FAB yet, so this call does nothing with the FAB.
        setMiniFabsVisible(startTime > 0);
        mPrefs.registerOnSharedPreferenceChangeListener(mPrefChangeListener);


        //设置图片分割间距，默认8dp，默认对应attr属性中divider_line_size
        mNineGridView.setDividerLineSize(TypedValue.COMPLEX_UNIT_DIP, 2);
        //设置是否开启编辑模式，默认false，对应attr属性中enable_edit_mode
        mNineGridView.setEnableEditMode(false);
        //设置水平方向上有多少列，默认3，对应attr属性中horizontal_child_count
        mNineGridView.setHorizontalChildCount(3);
        //设置非编辑模式下，只有一张图片时的尺寸，默认都为0，当宽高都非0才生效，且不会超过NineGridView内部可用总宽度，对应attr属性中single_image_width、single_image_height
        mNineGridView.setSingleImageSize(TypedValue.COMPLEX_UNIT_DIP, 150, 200);
        imageLoader = new GlideDisplayer();
        mAdapter = new DefaultNgvAdapter<ImageBean>(100, imageLoader, getActivity());

        mNineGridView.setAdapter(mAdapter);

        download();

        mAdapter.setOnChildClickListener(new DefaultNgvAdapter.OnChildClickedListener<ImageBean>()
        {
            @Override
            public void onPlusImageClicked(ImageView plusImageView, int dValueToLimited)
            {

            }

            @Override
            public void onContentImageClicked(@NonNull int targetNum, String targetPath, @NonNull ContentInfoCompat source, int width, int height)
            {

                String tag2 = (String)source.getClip().getDescription().getLabel();
                int sourceNum = Integer.parseInt(tag2);
                NgvChildImageView sourceD = (NgvChildImageView)mNineGridView.getChildAt(sourceNum);
                String sourcePath = (String)source.getClip().getItemAt(0).getText();

                NgvChildImageView targetD = (NgvChildImageView)mNineGridView.getChildAt(targetNum);


                Log.e("ImagePicker", "come here");
                //target
                imageLoader.load(sourcePath, targetD,
                        width, height);
                //source
                imageLoader.load(targetPath, sourceD,
                        width, height);
            }

            @Override
            public void onImageDeleted(int position, ImageBean data)
            {
            }
        });

        return view;
    }

    private void download( ) {
//        final Resources resources = getResources();
//        final SharedPreferencesUtil sp = new SharedPreferencesUtil(activity);
        String token = "test-token";

        Log.i(TAG, "enter here " );
        ipStr = "http://api.punengshuo.com";
        Log.i(TAG, ipStr + HttpUrl.DOWNLOAD_REQUEST );
        OKHttp3Util.getAsyn(ipStr + HttpUrl.DOWNLOAD_REQUEST, token, new OKHttp3Util.ResultCallback<JsonElement>() {
            @Override
            public void onError(Request request, Exception e) {
                Log.e(TAG, "请求失败=" + e.toString());
            }

            @Override
            public void onResponse(JsonElement response) {
                Log.e(TAG,"成功--->" + response.toString());

                PeerResultVO result = GsonUtil.GsonToBean(response.toString(), PeerResultVO.class);

                List<ImageBean> list = new ArrayList<>();

                for(String str : result.getData().getPiecces()){
                    ImageBean imageBean = new ImageBean();
//                    imageBean.setImageId(imageId);
                    imageBean.setImagePath(str);
//                    imageBean.setLastModified(ImagePickerComUtils.isNotEmpty(lastModify) ? Long.valueOf(lastModify) : 0);
                    imageBean.setWidth(200);
                    imageBean.setHeight(200);
//                    imageBean.setFolderId(folderId);
                    list.add(imageBean);
                }

                mAdapter.addDataList(list);

            }
        });
        Log.i(TAG, "enter end " );
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // TOneverDO: Move to onCreate(). When the device rotates, onCreate() _is_ called,
        // but trying to find the FAB in the Activity's layout will fail, and we would get back
        // a null reference. This is probably because this Fragment's onCreate() is called
        // BEFORE the Activity's onCreate.
        // TODO: Any better alternatives to control the Activity's FAB from here?
        mActivityFab = new WeakReference<>((FloatingActionButton) getActivity().findViewById(R.id.fab));
        // There is no documentation for isMenuVisible(), so what exactly does it do?
        // My guess is it checks for the Fragment's options menu. But we never initialize this
        // Fragment with setHasOptionsMenu(), let alone we don't actually inflate a menu in here.
        // My guess is when this Fragment becomes actually visible, it "hooks" onto the menu
        // options "internal API" and inflates its menu in there if it has one.
        //
        // To us, this just makes for a very good visibility check.
        if (savedInstanceState != null ) {
            // This is a pretty good indication that we just rotated.
            // isMenuVisible() filters out the case when you rotate on page 1 and scroll
            // to page 2, the icon will prematurely change; that happens because at page 2,
            // this Fragment will be instantiated for the first time for the current configuration,
            // and so the lifecycle from onCreate() to onActivityCreated() occurs. As such,
            // we will have a non-null savedInstanceState and this would call through.
            //
            // The reason when you open up the app for the first time and scrolling to page 2
            // doesn't prematurely change the icon is the savedInstanceState is null, and so
            // this call would be filtered out sufficiently just from the first check.
            syncFabIconWithStopwatchState(isStopwatchRunning());
        }
    }

    /**
     * If the user navigates away, this is the furthest point in the lifecycle
     * this Fragment gets to. Here, the view hierarchy returned from onCreateView()
     * is destroyed--the Fragment itself is NOT destroyed. If the user navigates back
     * to this tab, this Fragment goes through its lifecycle beginning from onCreateView().
     * <p/>
     * TODO: Verify that members are not reset.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "onDestroyView()");
        mPrefs.unregisterOnSharedPreferenceChangeListener(mPrefChangeListener);
    }


    @Override
    public void onPageSelected() {
        setMiniFabsVisible(getLongFromPref(KEY_START_TIME) > 0);
        syncFabIconWithStopwatchState(isStopwatchRunning());
    }

    @Override
    protected int contentLayout() {
        return R.layout.fragment_stopwatch;
    }

    @OnClick(R.id.new_lap)
    void addNewLap() {
        Intent serviceIntent = new Intent(getActivity(), StopwatchNotificationService.class)
                .setAction(StopwatchNotificationService.ACTION_ADD_LAP);
        getActivity().startService(serviceIntent);
    }

    @OnClick(R.id.stop)
    void stop() {
        // Remove the notification. This will also write to prefs and clear the laps table.
        Intent stop = new Intent(getActivity(), StopwatchNotificationService.class)
                .setAction(StopwatchNotificationService.ACTION_STOP);
        getActivity().startService(stop);
    }

    private void setMiniFabsVisible(boolean visible) {
        int vis = visible ? View.VISIBLE : View.INVISIBLE;
        mNewLapButton.setVisibility(vis);
        mStopButton.setVisibility(vis);
    }

    private void syncFabIconWithStopwatchState(boolean running) {
        mActivityFab.get().setImageDrawable(running ? mPauseDrawable : mStartDrawable);
    }

    private double getCurrentLapProgressRatio(Lap currentLap, Lap previousLap) {
        if (previousLap == null)
            return 0;
        // The cast is necessary, or else we'd have integer division between two longs and we'd
        // always get zero since the numerator will always be less than the denominator.
        return remainingTimeBetweenLaps(currentLap, previousLap) / (double) previousLap.elapsed();
    }

    private long remainingTimeBetweenLaps(Lap currentLap, Lap previousLap) {
        if (currentLap == null || previousLap == null)
            return 0;
        // TODO: Should we check if the subtraction results in negative number, and return 0?
        return previousLap.elapsed() - currentLap.elapsed();
    }

    /**
     * @return the state of the stopwatch when we're in a resumed and visible state,
     * or when we're going through a rotation
     */
    private boolean isStopwatchRunning() {
        return mChronometer.isRunning() || mPrefs.getBoolean(KEY_CHRONOMETER_RUNNING, false);
    }

    private long getLongFromPref(String key) {
        return mPrefs.getLong(key, 0);
    }

    private final OnSharedPreferenceChangeListener mPrefChangeListener = new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // We don't care what key-value pair actually changed, just configure all the views again.
            long startTime = sharedPreferences.getLong(KEY_START_TIME, 0);
            long pauseTime = sharedPreferences.getLong(KEY_PAUSE_TIME, 0);
            boolean running = sharedPreferences.getBoolean(KEY_CHRONOMETER_RUNNING, false);
            setMiniFabsVisible(startTime > 0);
            syncFabIconWithStopwatchState(running);
            // ==================================================
            // TOneverDO: Precede setMiniFabsVisible()
            if (startTime == 0) {
                startTime = SystemClock.elapsedRealtime();
            }
            // ==================================================

            // If we're resuming, the pause duration is already added to the startTime.
            // If we're pausing, then the chronometer will be stopped and we can use
            // the startTime that was originally set the last time we were running.
            //
            // We don't need to add the pause duration if we're pausing because it's going to
            // be negligible at this point.
//            if (pauseTime > 0) {
//                startTime += SystemClock.elapsedRealtime() - pauseTime;
//            }
            mChronometer.setBase(startTime);
            mChronometer.setStarted(running);

        }
    };

}
