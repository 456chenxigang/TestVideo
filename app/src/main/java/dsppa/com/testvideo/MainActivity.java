package dsppa.com.testvideo;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import org.videolan.libvlc.media.VideoView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import cn.bmob.v3.Bmob;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private VideoView mVideoView;
    private VideoInfo mVideoInfo;

    // Video controller
    private View mVideoControllerLayout;
    private View mVideoControllerRootView;
    private TextView mVideoControllerVideoTitle;
    private TextView mVideoControllerCurrentTime;
    private TextView mVideoControllerTotalTime;
    private SeekBar mVideoControllerVideoSeekBar;
    private ImageButton mVideoControllerPlayOrPause;
    private ImageButton mVideoControllerVideoLock;
    private ImageButton mVideoControllerSettings;
    private boolean mIsLocked;
    private boolean mIsPaused;
    private boolean mIsTouchingVideoSeekBar;
    private boolean mIsVideoControllerShowing;
    private boolean mIsOnlyEnglishSubtitle;
    private Timer mHideVideoControllerTimer;
    private int mDefaultLockImageSize;
    private TextView mTimedSubtitleView;
    private TextView mTranslationView;

    // Handler message
    private final int MSG_HIDE_VIDEO_CONTROLLER = 0;
    private final int MSG_HIDE_EXPLANATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //设置全屏
        View decorView = getWindow().getDecorView();
        int options = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(options);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mVideoView = (VideoView) findViewById(R.id.video_view);

        // Video controller
        mVideoControllerLayout = findViewById(R.id.video_controller_layout);
        mVideoControllerRootView = findViewById(R.id.video_controller);
        mVideoControllerVideoTitle = (TextView) mVideoControllerRootView.findViewById(R.id.video_controller_video_title);
        mVideoControllerCurrentTime = (TextView) mVideoControllerRootView.findViewById(R.id.video_controller_current_time);
        mVideoControllerTotalTime = (TextView) mVideoControllerRootView.findViewById(R.id.video_controller_total_time);
        mVideoControllerVideoSeekBar = (SeekBar) mVideoControllerRootView.findViewById(R.id.video_controller_seek_bar);
        mVideoControllerPlayOrPause = (ImageButton) mVideoControllerRootView.findViewById(R.id.video_controller_play_pause);
        mVideoControllerVideoLock = (ImageButton) mVideoControllerRootView.findViewById(R.id.video_controller_lock);
        mVideoControllerSettings = (ImageButton) mVideoControllerRootView.findViewById(R.id.video_controller_settings);
        mTimedSubtitleView = (TextView) findViewById(R.id.timed_subtitle_view);
        mTranslationView = (TextView) findViewById(R.id.word_translation_view);

        mTimedSubtitleView.setVisibility(View.INVISIBLE);
        mVideoControllerRootView.setVisibility(View.INVISIBLE);
        mTranslationView.setVisibility(View.INVISIBLE);

        mVideoInfo = new VideoInfo();
        mVideoInfo.setTitle("无标题");
        mVideoInfo.setDisplayName("测试rtp");
        mVideoInfo.setPath("http://baobab.wdjcdn.com/14564977406580.mp4");
        //mVideoInfo.setPath("/storage/emulated/0/tencent/QQfile_recv/sss.mp4");
        //mVideoInfo.setPath("/storage/emulated/0/tencent/QQfile_recv/zhpt.sdp");

        //mVideoView.setVideoPath(mVideoInfo.getPath());
        mVideoView.setVideoURI(Uri.parse(mVideoInfo.getPath()));
        mVideoView.setOnPreparedListener(mOnPreparedListener);
        mVideoView.start();

        Bmob.initialize(this,"506fd47670d6584b309652f9f8df09be");
    }

    @Override
    protected void onPause() {
        mVideoView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        if(!mIsPaused) {
            mVideoView.resume();
        }

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mVideoView.release();
        super.onDestroy();
    }

    private void initVideoController() {
        Log.e(TAG,"initVideoController");
        mVideoControllerLayout.setOnClickListener(mVideoControllerOnClickListener);
        mVideoControllerVideoTitle.setText(mVideoInfo.getDisplayName());
        mVideoControllerTotalTime.setText(getTimeDisplayString(mVideoView.getDuration()));
        mVideoControllerPlayOrPause.setOnClickListener(mOnPlayOrPauseClickListener);
        mVideoControllerVideoLock.setOnClickListener(mOnLockClickListener);
        mVideoControllerSettings.setOnClickListener(mOnSettingsClickListener);


        if(mVideoView.canSeekForward() || mVideoView.canSeekBackward()) {
            mVideoControllerVideoSeekBar.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        } else {
            mVideoControllerVideoSeekBar.setEnabled(false);
        }

        mVideoView.setOnCurrentTimeUpdateListener(mOnCurrentTimeUpdateListener);
        mVideoView.setOnCompletionListener(mOnCompletionListener);
        mVideoView.setOnErrorListener(new VideoView.OnErrorListener() {
            @Override
            public void onError() {
                Log.e(TAG,"onError");
            }
        });
    }

    private void startHideVideoControllerTimer() {
        if(mHideVideoControllerTimer != null) {
            mHideVideoControllerTimer.cancel();
        }

        mHideVideoControllerTimer = new Timer();

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.obtainMessage(MSG_HIDE_VIDEO_CONTROLLER).sendToTarget();
            }
        };

        mHideVideoControllerTimer.schedule(timerTask, 3000);
    }


    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_HIDE_VIDEO_CONTROLLER:
                    hideVideoController();

                    break;
                case MSG_HIDE_EXPLANATION:
                    mTranslationView.setVisibility(View.INVISIBLE);

                    break;
            }
        }
    };

    private void showVideoController() {
        mVideoControllerRootView.setVisibility(View.VISIBLE);
        startHideVideoControllerTimer();
        mIsVideoControllerShowing = true;
    }

    private void hideVideoController() {
        mVideoControllerRootView.setVisibility(View.INVISIBLE);
        mIsVideoControllerShowing = false;
    }

    private View.OnClickListener mVideoControllerOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(mIsVideoControllerShowing) {
                hideVideoController();
            } else {
                showVideoController();
            }
        }
    };

    private VideoView.OnPreparedListener mOnPreparedListener = new VideoView.OnPreparedListener() {
        @Override
        public void onPrepared() {
            initVideoController();
        }
    };

    private VideoView.OnCompletionListener mOnCompletionListener = new VideoView.OnCompletionListener() {
        @Override
        public void onCompletion() {
            Log.e(TAG,"onCompletion");
            mVideoView.setVideoPath(mVideoInfo.getPath());
            mVideoView.resume();
        }
    };


    private VideoView.OnCurrentTimeUpdateListener mOnCurrentTimeUpdateListener = new VideoView.OnCurrentTimeUpdateListener() {
        @Override
        public void onCurrentTimeUpdate(int currentTime) {
            if(!mIsTouchingVideoSeekBar) {
                mVideoControllerCurrentTime.setText(getTimeDisplayString(currentTime));

                if(mVideoView.getDuration() != 0) {
                    mVideoControllerVideoSeekBar.setProgress(currentTime * 100 / mVideoView.getDuration());
                }
            }
        }
    };

    private View.OnClickListener mOnPlayOrPauseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mIsPaused = !mIsPaused;

            if(mIsPaused) {
                mVideoView.pause();
                mVideoControllerPlayOrPause.setImageResource(R.drawable.video_controller_play);
            } else {
                mVideoView.resume();
                mVideoControllerPlayOrPause.setImageResource(R.drawable.video_controller_pause);
            }

            startHideVideoControllerTimer();
        }
    };

    private View.OnClickListener mOnLockClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

//            if (true){
//                int [] array = new int[3];
//                for (int i = 0; i < array.length+1;i ++){
//                    Log.e(TAG,array[i]+"");
//                }
//                return;
//            }

            View videoControllerTop = mVideoControllerRootView.findViewById(R.id.video_controller_top);
            View videoControllerBottom = mVideoControllerRootView.findViewById(R.id.video_controller_bottom);
            View videoControllerRight = mVideoControllerRootView.findViewById(R.id.video_controller_right);

            mIsLocked = !mIsLocked;
            int lockImageSize;

            if(mIsLocked) {
                mDefaultLockImageSize = mVideoControllerVideoLock.getWidth();
                lockImageSize = (int)(mDefaultLockImageSize * 1.5);

                videoControllerTop.setVisibility(View.INVISIBLE);
                videoControllerBottom.setVisibility(View.INVISIBLE);
                videoControllerRight.setVisibility(View.INVISIBLE);
            } else {
                lockImageSize = mDefaultLockImageSize;

                videoControllerTop.setVisibility(View.VISIBLE);
                videoControllerBottom.setVisibility(View.VISIBLE);
                videoControllerRight.setVisibility(View.VISIBLE);
            }

            ViewGroup.LayoutParams layoutParams = mVideoControllerVideoLock.getLayoutParams();
            layoutParams.width = lockImageSize;
            layoutParams.height = lockImageSize;
            mVideoControllerVideoLock.setLayoutParams(layoutParams);

            startHideVideoControllerTimer();
        }
    };

    private View.OnClickListener mOnSettingsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mIsOnlyEnglishSubtitle = !mIsOnlyEnglishSubtitle;

            mVideoView.updateTimedText();
        }
    };

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean manual) {
            if(manual) {
                mVideoControllerCurrentTime.setText(
                        getTimeDisplayString(progress * mVideoView.getDuration() / 100));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mIsTouchingVideoSeekBar = true;

            if(mHideVideoControllerTimer != null) {
                mHideVideoControllerTimer.cancel();
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mIsTouchingVideoSeekBar = false;
            mVideoView.seekTo(seekBar.getProgress() * mVideoView.getDuration() / 100);

            startHideVideoControllerTimer();
        }
    };

    private String getTimeDisplayString(long milliSeconds) {
        String timeFormat = milliSeconds > 60 * 60 * 1000 ? "HH:mm:ss" : "mm:ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(timeFormat, Locale.ENGLISH);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        return simpleDateFormat.format(milliSeconds);
    }
}
