package dsppa.com.testvideo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import org.videolan.libvlc.media.VideoView;

public class PlayVideoActivity extends AppCompatActivity {

    private VideoView mVideoView;
    private VideoInfo mVideoInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);


        mVideoInfo = new VideoInfo();
        mVideoInfo.setTitle("无标题");
        mVideoInfo.setDisplayName("测试rtp");
        mVideoInfo.setPath("/storage/emulated/0/tencent/QQfile_recv/sss.mp4");
        //mVideoInfo.setPath("http://baobab.wdjcdn.com/14564977406580.mp4");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN , WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mVideoView = (VideoView) findViewById(R.id.video_view);


        String videoPath = mVideoInfo.getPath();
         mVideoView.setVideoPath(videoPath);
        //mVideoView.setVideoURI(Uri.parse(videoPath));

        //mVideoView.setOnPreparedListener(mOnPreparedListener);

        mVideoView.start();
    }

    @Override
    protected void onPause() {
        mVideoView.pause();
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        mVideoView.release();
        super.onDestroy();
    }
}
