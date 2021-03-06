package daily.zjrb.com.daily_vr.controller;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.Image;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

import daily.zjrb.com.daily_vr.AnalyCallBack;
import daily.zjrb.com.daily_vr.R;

/**
 * @author: lujialei
 * @date: 2018/5/14
 * @describe:提示性UI和声音控制
 */


public class HintController extends RelativeLayout{
    private CheckBox playerVolumn;
    private LinearLayout hintBufferProgress;
    private Activity activity;
    private AnalyCallBack analyCallBack;
    private LinearLayout guideArea;
    public boolean hasGuided;
    int currentVolume;
    android.os.Handler mHandler = new android.os.Handler();
    private AudioManager audioManager;
    private ImageView ivBuffering;


    public HintController(Activity activity, AnalyCallBack analyCallBack) {
        super(activity);
        this.activity = activity;
        this.analyCallBack = analyCallBack;
        audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        initView(activity);
        initListener();
    }

    public void volumeChanged() {
        AudioManager audioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(currentVolume ==0){
            playerVolumn.setChecked(true);
        }else {
            playerVolumn.setChecked(false);
        }
    }



    private void initListener() {
        //调节音量
        playerVolumn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {


            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){//静音
                    currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    audioManager.setStreamMute(AudioManager.STREAM_MUSIC,true);
                    analyCallBack.mute();
                }else {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,currentVolume,0);
                    analyCallBack.openVolumn();
                }
            }
        });

        guideArea.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                guideArea.setVisibility(GONE);
                return false;
            }
        });



    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.vr_layout_hint_controller,this,true);
        playerVolumn = (CheckBox) view.findViewById(R.id.player_ic_volume);
        hintBufferProgress = (LinearLayout) view.findViewById(R.id.player_buffer_progress);
        guideArea = (LinearLayout) view.findViewById(R.id.ll_vr_guide);
        ivBuffering = (ImageView) view.findViewById(R.id.iv_buffering);
        ObjectAnimator anim = ObjectAnimator.ofFloat(ivBuffering, "rotation", 0f, 360f);
        anim.setDuration(900);
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatMode(ValueAnimator.RESTART);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.start();
    }

    public void showBuffering(boolean visiable) {
        hintBufferProgress.setVisibility(visiable?VISIBLE:GONE);
    }

    public boolean isGUideShowing(){
        return guideArea.getVisibility() == VISIBLE;
    }

    public void showGuide(){
        hasGuided = true;
        guideArea.setVisibility(VISIBLE);
        mHandler.postDelayed(new Runnable() {
               @Override
               public void run() {
                   guideArea.setVisibility(GONE);
               }
        },3000);

    }

    public void hideGuide() {
        guideArea.setVisibility(GONE);
    }


    public void showVolume(boolean b) {
        if(b){
            playerVolumn.setVisibility(VISIBLE);
        }else {
            playerVolumn.setVisibility(GONE);
        }

    }
}
