package daily.zjrb.com.daily_vr.player;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aliya.player.gravity.OrientationHelper;
import com.aliya.player.gravity.OrientationListener;
import com.utovr.ma;
import com.utovr.player.UVEventListener;
import com.utovr.player.UVInfoListener;
import com.utovr.player.UVMediaPlayer;
import com.utovr.player.UVMediaType;
import com.utovr.player.UVNetworkListenser;
import com.zjrb.core.utils.L;
import com.zjrb.core.utils.NetUtils;
import com.zjrb.core.utils.SettingManager;

import butterknife.ButterKnife;
import daily.zjrb.com.daily_vr.CalcTime;
import daily.zjrb.com.daily_vr.R;
import daily.zjrb.com.daily_vr.Utils;
import daily.zjrb.com.daily_vr.ui.ControllerContainer;

import static android.content.Context.CONNECTIVITY_SERVICE;

/**
 * @author: lujialei
 * @date: 2018/4/26
 * @describe: 横竖屏controller基类
 */


public class BaseController extends RelativeLayout implements UVEventListener, UVInfoListener {

    protected UVMediaPlayer player = null;
    protected CalcTime calcTime;
    Handler handler = new Handler();
    private boolean bufferResume = true;

    protected ViewGroup parent;
    private int mLastOrientation = -100;//上一次的方向记录
    private boolean switchFromUser;
    protected UVMediaType type;
    protected  String path;
    private boolean mCurrentIsLand;
    protected Activity activity;
    private int screenchange;
    ProgressController progressController;
    HintController hintController;
    PrepareController prepareController;

    public BaseController(UVMediaPlayer player, Activity activity, ViewGroup parent){
        super(activity);
        this.player = player;
        this.activity = activity;
        this.parent = parent;
        calcTime = new CalcTime();
        initView(activity);
        initListener();
    }


    public void initView(Context context){
        LayoutInflater.from(context).inflate(R.layout.vr_layout_controller, this, true);
        updatePositionTask = new UpdatePositionTask();
        //添加进度条控制器
        progressController = new ProgressController(context,player,activity);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        progressController.setLayoutParams(params);
        addView(progressController);
        //添加提示UI和声音控制器
        hintController = new HintController(activity);
        hintController.setLayoutParams(params);
        addView(hintController);
        //添加播放和重播控制器
        prepareController = new PrepareController(activity);
        prepareController.setLayoutParams(params);
        addView(prepareController);


    }

    private void initListener() {
        prepareController.setOnPrepareControllerListener(new PrepareController.OnPrepareControllerListener() {
            @Override
            public void onStartClicked() {
                if(player.getCurrentPosition() != 0){//在播放过程中提示网络变化 直接播放
                    progressController.play();
                    return;
                }
                check4G();
            }

            @Override
            public void onRestartClicked() {
                player.replay();
            }
        });

        progressController.setOnProgressControllerListener(new ProgressController.OnProgressControllerListener() {
            @Override
            public void onChangeOrientation(boolean b) {
                changeOrientation(b);
            }

            @Override
            public void onIsFromUserSwitch(boolean b) {
                switchFromUser = b;
            }
        });


        player.setNetWorkListenser(new UVNetworkListenser() {//播放过程中的网络变化
            @Override
            public void onNetworkChanged(int i) {
                if(i == UVNetworkListenser.NETWORK_WIFI){//wifi

                }else if(i == UVNetworkListenser.NETWORK_MOBILE_2G || i == UVNetworkListenser.NETWORK_MOBILE_3G ||i == UVNetworkListenser.NETWORK_MOBILE_4G){
                    if(player.isPlaying()){
                        prepareController.setNetHintText("用流量播放");
                        progressController.initPlay();
                    }
                }
            }
        });

        OrientationHelper orientationHelper = new OrientationHelper();
        orientationHelper.registerListener(parent.getContext(), new OrientationListener() {
            @Override
            public void onOrientation(int orientation) {
                if(mLastOrientation != orientation){//方向变化后
                    mLastOrientation = orientation;
                    switchFromUser = false;
                }

                try {
                    //屏幕旋转是否开启 0未开启 1开启
                    screenchange = Settings.System.getInt(activity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
                if(orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE && !switchFromUser){//横屏翻转
                    if(screenchange == 1){
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        changeOrientation(true);
                    }
                }else if(orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT && !switchFromUser){//竖屏翻转
                    if(screenchange == 1){
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        changeOrientation(false);
                    }
                }else if(orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && !switchFromUser){//横屏
                    if(screenchange == 1){
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        changeOrientation(true);
                    }
                }else if(orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && !switchFromUser){//竖屏
                    if(screenchange == 1){
                        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        changeOrientation(false);
                    }

                }
            }
        });

        progressController.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(prepareController.getUIState()){
                    return;
                }
                if(mControllerBarTask == null){
                    mControllerBarTask = new ControllerBarTask();
                }
                progressController.switchLand(mCurrentIsLand);
                handler.removeCallbacks(mControllerBarTask);
                handler.postDelayed(mControllerBarTask,3000);
            }
        });

    }

    public void setSource(UVMediaType type, String path) {
        this.type = type;
        this.path = path;
        if (SettingManager.getInstance().isAutoPlayVideoWithWifi() && NetUtils.isWifi()){//自动播放
            player.setSource(type,path);
        }else {
            prepareController.showStartView();

        }
    }

    private void check4G() {
        if(NetUtils.isMobile() && prepareController.shoudPlay()){//用流量提醒的状态下点击播放 直接播放
            player.setSource(type,path);
            return;
        }
        if(NetUtils.isWifi()){//wifi情况下点击就播放
            player.setSource(type,path);
            return;
        }
        if(NetUtils.isMobile()){
            prepareController.setNetHintText("用流量播放");
        }
    }

    @Override
    public void onStateChanged(int playbackState) {
        {
            Log.i("utovr", "+++++++ playbackState:" + playbackState);
            switch (playbackState) {
                case UVMediaPlayer.STATE_PREPARING:
                    break;
                case UVMediaPlayer.STATE_BUFFERING:
                    if (player != null && player.isPlaying()) {
                        bufferResume = true;
                        hintController.showBuffering(true);
                    }
                    break;
                case UVMediaPlayer.STATE_READY:
                    // 设置时间和进度条
                    if (bufferResume) {
                        bufferResume = false;
                        hintController.showBuffering(false);
                    }
                    break;
                case UVMediaPlayer.STATE_ENDED:
                    //这里是循环播放，可根据需求更改
                    showEndView();
                    break;
                case UVMediaPlayer.TRACK_DISABLED:
                case UVMediaPlayer.TRACK_DEFAULT:
                    break;
            }
        }
    }

    //播放结束
    private void showEndView() {
        prepareController.showEnd();

    }

    @Override
    public void onError(Exception e, int i) {

    }

    @Override
    public void onVideoSizeChanged(int i, int i1) {

    }

    @Override
    public void onBandwidthSample(int i, long l, long l1) {

    }

    @Override
    public void onLoadStarted() {

    }

    @Override
    public void onLoadCompleted() {

    }

    //更新进度条
    UpdatePositionTask updatePositionTask;
    public void updateCurrentPosition(final long position) {
        calcTime.calcTime(player);
        final int bufferProgress = calcTime.calcSecondaryProgress(progressController.getMax());
        updatePositionTask.setPosition((int) position);
        updatePositionTask.setBufferProgress(bufferProgress);
        handler.post(updatePositionTask);
    }

    //隐藏进度条
    public  ControllerBarTask mControllerBarTask;
    class ControllerBarTask implements  Runnable{

        @Override
        public void run() {
            progressController.hideUI();
        }
    }

//    更新ui的任务
    class UpdatePositionTask implements Runnable{

        public void setPosition(int position) {
            this.position = position;
        }

        public void setBufferProgress(int bufferProgress) {
            this.bufferProgress = bufferProgress;
        }

        private int position;
        private int bufferProgress;

        @Override
        public void run() {
            progressController.updatePosition(player.getDuration(),position,bufferProgress,calcTime.formatDuration(),calcTime.formatPosition());
        }
    }

    public void changeOrientation(boolean isLandscape) {
        if(mCurrentIsLand != isLandscape){//屏幕横竖屏发生变化
            mCurrentIsLand = isLandscape;
            progressController.hideUI();
            if (isLandscape) {
//            切换横屏
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
                parent.setLayoutParams(lp);
            }
            else
            {
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                int smallPlayH = Utils.getSmallPlayHeight(activity.getWindow());
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, smallPlayH);
                parent.setLayoutParams(lp);
            }
        }

    }

    //播放前的网络变化监听
    public void onNetWorkChanged() {
        if(NetUtils.isMobile()){
            prepareController.setNetHintText("用流量播放");
        }
        if(prepareController.isNetHintShowing()){
            if(NetUtils.isWifi()){
                prepareController.setNetHintText("已切换至wifi");
            }
        }
    }

}
