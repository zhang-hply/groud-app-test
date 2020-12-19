package com.dji.ux.sample;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJILatLng;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import dji.common.Stick;
import dji.common.battery.BatteryState;
import dji.common.battery.WarningRecord;
import dji.common.error.DJIDiagnosticError;
import dji.common.error.DJIError;
import dji.common.error.DJIFlightHubError;
import dji.common.remotecontroller.HardwareState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.BatteryKey;
import dji.keysdk.CameraKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.GimbalKey;
import dji.keysdk.KeyManager;
import dji.common.gimbal.*;
import dji.keysdk.callback.GetCallback;
import dji.keysdk.callback.KeyListener;
import dji.sdk.base.DJIDiagnostics;
import dji.sdk.battery.Battery;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;

import static com.dji.ux.sample.DensityUtil.point_number;

/**
 * Activity that shows all the UI elements together
 */
public class CompleteWidgetActivity extends Activity {
    private MapWidget mapWidget;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private RelativeLayout primaryVideoView;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;

    private TextView voltage_battery;
    private TextView voltage_battery_lowlevel;
    private int cell_voltage;

    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;

    private TextView textView_distance_front;
    private TextView textView_distance_rear;
    private TextView textView_distance_left;
    private TextView textView_distance_right;
    private TextView textView_distance_up;
    private TextView textView_distance_bottom;
    private TextView texView_depth_of_water;

    private FlightController mFlightController;
    private Aircraft mAircraft;
    private byte[] data = new byte[4];
    private Switch aSwitch;
    private Switch takeoffSwitch;

    float point[][] = new float[point_number][2];

    //云台角度
    private double yaw = 1.5 / 180.0 * Math.PI;
    private double yaw_init;
    private double yaw_quad;
    private double pitch_quad;
    private double pitch = 0 / 180.0 * Math.PI;
    //喷管角度
    private double nozzle_angle = 12.0 / 180.0 * Math.PI;

    private TextView automatic_infor;

    private double water_level = 0;
    private int init_count = 0;
    private double ratio = 0;
    private int init_count1 = 0;
    private double ratio1 = 0;

    private String[] text_array;

    private int leftPosition[] = {0, 0};
    private int rightPosition[] = {0, 0};

    private ValueAnimator mAnimator;
    private Button mView;

    private int flag = 0;
    private boolean flag_previou[] = {false, false, false, false, false, false};
    private boolean flag_present[] = {false, false, false, false, false, false};
    private int count_add_contentview = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_widgets);

        height = DensityUtil.dip2px(this, 100);
        width = DensityUtil.dip2px(this, 150);
        margin = DensityUtil.dip2px(this, 12);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

        mapWidget = findViewById(R.id.map_widget);
        mapWidget.initAMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull DJIMap map) {
                map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(DJILatLng latLng) {
                        onViewClick(mapWidget);
                    }
                });
            }
        });
        mapWidget.onCreate(savedInstanceState);

        initview();

        fpvWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewClick(fpvWidget);
            }
        });
     //   swapVideoSource();
        secondaryFPVWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapVideoSource();
            }
        });
        updateSecondaryVideoVisibility();
        //监听大疆云台角度
        mAircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
        FlightControllerKey yawFlightControllerKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_YAW);
        KeyListener yawAttitudeQuadListener = new KeyListener() {
            @Override
            public void onValueChange(Object o, Object o1) {
                yaw_quad = (double)o1;
                Log.d("test_yaw_quad", yaw_quad + "度");
            }
        };
        KeyManager.getInstance().addListener(yawFlightControllerKey, yawAttitudeQuadListener);
        GetCallback yawGetCallback = new GetCallback() {
            @Override
            public void onSuccess(Object o) {
                yaw_quad = (double)o;
                Log.d("test_call_yaw", yaw_quad + "度");
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        };
        KeyManager.getInstance().getValue(yawFlightControllerKey, yawGetCallback);

        FlightControllerKey pitchFlightControllerKey = FlightControllerKey.create(FlightControllerKey.ATTITUDE_PITCH);
        KeyListener pitchAttitudeQuadListener = new KeyListener() {
            @Override
            public void onValueChange(Object o, Object o1) {
                pitch_quad = (double)o1;
                Log.d("test_pitch_quad", pitch_quad + "度");
            }
        };
        KeyManager.getInstance().addListener(pitchFlightControllerKey, pitchAttitudeQuadListener);
        GetCallback pitchGetCallback = new GetCallback() {
            @Override
            public void onSuccess(Object o) {
                pitch_quad = (double)o;
                Log.d("test_call_pitch", pitch_quad + "度");
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        };
        KeyManager.getInstance().getValue(pitchFlightControllerKey, pitchGetCallback);

        GimbalKey gimbalKey = GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES);
        KeyListener attitudekeyListener = new KeyListener() {
            @Override
            public void onValueChange(Object o, Object o1) {
                pitch = -(((Attitude) o1).getPitch() - pitch_quad) / 180.0 * Math.PI;
                yaw =  -(((Attitude) o1).getYaw() - yaw_quad - 1.5) / 180.0 * Math.PI;
//                if(flag == 0){
//                    yaw_init = ((Attitude) o1).getYaw();
//                    Log.d("CompletewidgetActivity","getListener-yaw_init: " + yaw_init);
//                }
//                else{
//                    yaw = -(((Attitude) o1).getYaw() - yaw_init) / 180.0 * Math.PI;
//                    Log.d("CompletewidgetActivity","getListener-pitch: " + pitch * 180.0 / Math.PI + "yaw: " + yaw * 180.0 / Math.PI);
//                }
//                flag = flag + 1;
                Log.d("test_real_yaw", yaw * 180 /  Math.PI + "度");
                Log.d("test_yaw", ((Attitude) o1).getYaw() + "度");
         //       addContentView(new CustomView1(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
                Log.d("water_curve_gimbal", "Handle::" + "Pitch: " + pitch * 180.0 / Math.PI + ". Yaw: " +
                        yaw * 180.0 / Math.PI + "nozzle_angle:" + nozzle_angle * 180.0 / Math.PI+
                        ", u:" + point[0][0] + ", v:" + point[0][1]);
            }
        };
        KeyManager.getInstance().addListener(gimbalKey, attitudekeyListener);
        //落点标定曲线
        //mtimer.schedule(task, 0, 5000);
        addContentView(new CustomView1(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
        Log.d("water_curve_create", "Handle::" + "Pitch: " + pitch * 180.0 / Math.PI + ". Yaw: " +
                yaw * 180.0 / Math.PI + "nozzle_angle:" + nozzle_angle * 180.0 / Math.PI+
                ", u:" + point[0][0] + ", v:" + point[0][1]);
        //电池电压显示
        BatteryKey batteryKey_level = BatteryKey.create(BatteryKey.LEVEL_1_CELL_VOLTAGE_THRESHOLD);
        GetCallback getCallback = new GetCallback() {
            @Override
            public void onSuccess(Object o) {
                double vol_low_level = ((int) o) / 1000.0;
                vol_low_level = vol_low_level * cell_voltage;
                voltage_battery_lowlevel.setText(vol_low_level +"v");
                Log.d("LowVoltageBehavior",  ((int) o) + "mv");
            }

            @Override
            public void onFailure(DJIError djiError) {

            }
        };
        KeyManager.getInstance().getValue(batteryKey_level, getCallback);


        if(mAircraft != null && mAircraft.isConnected()) {
            Log.d("Message from onboard", "connect succucess");
            Battery mbattery = mAircraft.getBattery();
            cell_voltage = mbattery.getNumberOfCells();
            //电池实时电压显示
            mbattery.setStateCallback(new BatteryState.Callback() {
                @Override
                public void onUpdate(BatteryState batteryState) {
                    double voltage_d = (batteryState.getVoltage()) / 1000.0;
                    if(voltage_d > 44.04)
                        voltage_battery.setTextColor(Color.GREEN);
                    else
                        voltage_battery.setTextColor(Color.RED);
                    voltage_battery.setText(voltage_d + "V");

                    Log.d("voltage_batter value", voltage_d + "V");
                }
            });

            mAircraft.setDiagnosticsInformationCallback(new DJIDiagnostics.DiagnosticsInformationCallback() {
                @Override
                public void onUpdate(List<DJIDiagnostics> list) {
                    int i = 0;
                    for(DJIDiagnostics djiDiagnostics : list){
                        i++;
                        int x = djiDiagnostics.getCode();
                        Log.d("djiDiagnostics" + i,   x + ": " + djiDiagnostics.getReason());
                        Toast.makeText(CompleteWidgetActivity.this, djiDiagnostics.getReason(), Toast.LENGTH_LONG).show();
                    }
                }
            });

            //从onboard sdk接收数据
            mFlightController = mAircraft.getFlightController();
            mFlightController.setOnboardSDKDeviceDataCallback(new FlightController.OnboardSDKDeviceDataCallback() {
                @Override
                public void onReceive(byte[] bytes) {
                    String text = new String(bytes);
                    showToast(text);
                    Log.d("Message from onboard", text);
                }
            });
        }

        //喷杆角度输入框
        final EditText mEdittext = (EditText)findViewById(R.id.input_nozzel_angle);
        mEdittext.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mEdittext.setCursorVisible(true);
                String angle_string = mEdittext.getText().toString().trim();
                if(!angle_string.equals(""))
                    nozzle_angle = (12.0 - Double.parseDouble(angle_string)) / 180.0 * Math.PI;
                Log.d("EditText.getText", nozzle_angle+"deg");
                mEdittext.clearFocus();

           //     addContentView(new CustomView1(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
                Log.d("water_curve_nozzle", "Handle::" + "Pitch: " + pitch * 180.0 / Math.PI + ". Yaw: " +
                        yaw * 180.0 / Math.PI + ". nozzle_angle:" + nozzle_angle * 180.0 / Math.PI+
                        ", u:" + point[0][0] + ", v:" + point[0][1]);
                return false;
            }
        });

        final EditText mEdittext_input_length = (EditText)findViewById(R.id.input_insulator_length);
        mEdittext_input_length.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                mEdittext_input_length.setCursorVisible(true);
                String angle_string = mEdittext_input_length.getText().toString().trim();

                data[0] = (byte)'2';
                for(int i = 1; i < angle_string.length() + 1; i++)
                    data[i] = (byte)angle_string.charAt(i - 1);

                if(mAircraft != null && mAircraft.isConnected()){
                    Log.d("input_sendtoOnboard", angle_string + "INPUT" + data[0] + data[1] + data[2] + data[3]);
                    //发送数据到onboard
                    mFlightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {

                        }
                    });
                }
                else
                    Log.d("sendDataToOnboard", "no mAircraft");

                for(int i = 1; i < angle_string.length() + 1; i++)
                    data[i] = ' ';
                mEdittext_input_length.clearFocus();
                return false;
            }
        });

        addContentView(new WaterLevelView1(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
        addContentView(new WaterLevelView2(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
        //刹车开关
        aSwitch.setChecked(false);
        aSwitch.setSwitchTextAppearance(CompleteWidgetActivity.this, R.style.s_false);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Log.d("aSwitch", "R.style.s_true");
                    aSwitch.setSwitchTextAppearance(CompleteWidgetActivity.this, R.style.s_true);
                    data[0] = '0'; data[1] = '1';
                }
                else{
                    Log.d("aSwitch", "R.style.s_false");
                    aSwitch.setSwitchTextAppearance(CompleteWidgetActivity.this, R.style.s_false);
                    data[0] = '0'; data[1] = '0';
                }

                if(mAircraft != null && mAircraft.isConnected()){
                    Log.d("Switch_sendtoOnboard", "aSwitch" + data[0]);
                    //发送数据到onboard
                    mFlightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {

                        }
                    });
                }
                else
                    Log.d("sendDataToOnboard", "no mAircraft");
            }
        });

        takeoffSwitch.setChecked(false);
        takeoffSwitch.setSwitchTextAppearance(CompleteWidgetActivity.this, R.style.s_false);
        takeoffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    Log.d("takeoffSwitch", "R.style.s_true");
                    aSwitch.setSwitchTextAppearance(CompleteWidgetActivity.this, R.style.s_true);
                    data[0] = '1'; data[1] = '1';
                }
                else{
                    Log.d("takeoffSwitch", "R.style.s_false");
                    aSwitch.setSwitchTextAppearance(CompleteWidgetActivity.this, R.style.s_false);
                    data[0] = '1'; data[1] = '0';
                }

                if(mAircraft != null && mAircraft.isConnected()){
                    Log.d("Switch_sendtoOnboard", "taskcleanSwitch" + data[0]);
                    //发送数据到onboard
                    mFlightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {

                        }
                    });
                }
                else
                    Log.d("sendDataToOnboard", "no mAircraft");
            }
        });
        //animation_water();
        //遥控器内八进行弹窗提示--起飞前进行检查
//        if(mAircraft != null && mAircraft.isConnected()) {
//            RemoteController mRemoteController = mAircraft.getRemoteController();
//            mRemoteController.setHardwareStateCallback(new HardwareState.HardwareStateCallback() {
//                @Override
//                public void onUpdate(HardwareState hardwareState) {
//                    Stick mLeftStick = hardwareState.getLeftStick();
//                    Stick mRightStick = hardwareState.getRightStick();
//                    leftPosition[0] = mLeftStick.getHorizontalPosition();
//                    leftPosition[1] = mLeftStick.getVerticalPosition();
//                    rightPosition[0] = mRightStick.getHorizontalPosition();
//                    rightPosition[1] = mRightStick.getVerticalPosition();
//                    Log.d("mLeftStick", leftPosition[0] + " " + leftPosition[1] + ";" + rightPosition[0] + " " + rightPosition[1]);
//                    if (leftPosition[0] > 600 && leftPosition[1] < -600 && rightPosition[0] < -600 && rightPosition[1] < -600) {
//                        dialogShowBeforeTakeOff();
//                    }
//                }
//            });
//        }
    }

   //调试接受消息以及画框
    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
//                Log.d("Message_from_onboard", msg);
                String[] receive_array = msg.split(" ");
                String[] receive_array_without_flag = new String[receive_array.length - 1];
                String receive_flag = receive_array[0];
                for(int i = 0; i < receive_array.length - 1; i++)
                    receive_array_without_flag[i] = receive_array[i + 1];
                Log.d("receive_array_without", receive_array_without_flag[0]);
                if(receive_flag.equals("0")){
                    //障碍距离以及障碍显示框
                    Log.d("receive_array_obstacle", "obstacle");
                    text_array = receive_array_without_flag.clone();
                    String[] text_array_for_display = receive_array_without_flag;
                    for(int i = 0; i < 6; i++){
                        if (Double.parseDouble(text_array[i]) >= 39.0)
                            text_array_for_display[i] = "SAFE";
                        else
                            text_array_for_display[i] = text_array_for_display[i] + "m";
                    }
                    textView_distance_front.setText(text_array_for_display[0]);
                    textView_distance_rear.setText(text_array_for_display[1]);
                    textView_distance_left.setText(text_array_for_display[2]);
                    textView_distance_right.setText(text_array_for_display[3]);
                    textView_distance_up.setText(text_array_for_display[4]);
                    textView_distance_bottom.setText(text_array_for_display[5]);

                    for(int i = 0; i < 6; i++){
                        flag_previou[i] = flag_present[i];
                        if(Double.parseDouble(text_array[i]) > 6.0)
                            flag_present[i] = true;
                        else
                            flag_present[i] = false;
                    }

                    if((flag_previou[0] != flag_present[0])
                            | flag_previou[1] != flag_present[1]
                            | flag_previou[2] != flag_present[2]
                            | flag_previou[3] != flag_present[3]
                            | flag_previou[4] != flag_present[4]
                            | flag_previou[5] != flag_present[5]){
                        addContentView(new CustomView_obstacle(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
                        Log.d("obstacle_change", "zhang");
                    }
                    count_add_contentview++;
                    if(count_add_contentview % 100 == 0){
                        addContentView(new CustomView_obstacle(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
                        Log.d("obstacle_time", "ye");
                    }
                }
                else if(receive_flag.equals("1")){
                    //刹车开关反馈消息
                    Log.d("receive_array_aSwitch", receive_array_without_flag[0]);
                    if(receive_array_without_flag[0].equals("0"))
                        aSwitch.setChecked(false);
                }
                else if(receive_flag.equals("2")){
                    //水箱的水位深度
                    Log.d("receive_array", "texView_depth_of_water");
                    ratio = Double.parseDouble(receive_array_without_flag[0]);
                    int pre_init_count = init_count;
                    if(ratio <= 100.0 && ratio >= 85.0)
                        init_count = 0;
                    else if(ratio < 75.0 && ratio >= 60.0)
                        init_count = 1;
                    else if(ratio < 60.0 && ratio >= 40.0)
                        init_count = 2;
                    else if(ratio < 40.0 && ratio >= 20.0)
                        init_count = 3;
                    else if(ratio < 20.0 && ratio >=10.0)
                        init_count = 4;
                    else if(ratio < 10.0)
                        init_count = 5;
                    if(pre_init_count != init_count){
                        addContentView(new WaterLevelView1(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
                        Log.d("WaterLevelView", "No." + init_count);
                    }

                    ratio1 = Double.parseDouble(receive_array_without_flag[1]);
                    int pre_init_count1 = init_count1;
                    if(ratio1 <= 100.0 && ratio1 >= 85.0)
                        init_count1 = 0;
                    else if(ratio1 < 75.0 && ratio1 >= 60.0)
                        init_count1 = 1;
                    else if(ratio1 < 60.0 && ratio1 >= 40.0)
                        init_count1 = 2;
                    else if(ratio1 < 40.0 && ratio1 >= 20.0)
                        init_count1 = 3;
                    else if(ratio1 < 20.0 && ratio1 >= 10.0)
                        init_count1 = 4;
                    else if(ratio1 < 10.0)
                        init_count1 = 5;
                    if(pre_init_count1 != init_count1){
                        addContentView(new WaterLevelView2(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
                        Log.d("WaterLevelView", "No." + init_count);
                    }
                //    texView_depth_of_water.setText(receive_array_without_flag[0] + "m");
                }
                else if(receive_flag.equals("3")){
                    //刹车开关反馈消息
                    Log.d("receive_takeoffSwitch", receive_array_without_flag[0]);
                    if(receive_array_without_flag[0].equals("0"))
                        takeoffSwitch.setChecked(false);
                }
                else if(receive_flag.equals("4")){
                    Log.d("receive_automatic", receive_array_without_flag[0]);
                    Toast.makeText(CompleteWidgetActivity.this, msg.substring(2, msg.length()), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    void animation_water(){
        Log.d("animation_water", "enter function");
        mAnimator = ValueAnimator.ofObject(new BezierEvaluator(), new PointF(point[0][0], point[0][1]),
                new PointF(point[500- 1][0], point[500 - 1][1]));
        mAnimator.setDuration(1000);
        mAnimator.setInterpolator(new DecelerateInterpolator());
        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                PointF pointF = (PointF)animation.getAnimatedValue();
                mView.setX(pointF.x - 20);
                mView.setY(pointF.y - 20);
            }

        });
        //      mAnimator.setTarget(mView);
        mAnimator.setRepeatCount(100);
//        mAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mAnimator.start();
    }
    class BezierEvaluator implements TypeEvaluator<PointF> {
        @Override
        public PointF evaluate(float fraction, PointF startValue,
                               PointF endValue) {
            PointF mpoint = new PointF();
            mpoint.x = point[(int) (fraction * (point_number / 10 - 1))][0];
            mpoint.y = point[(int) (fraction * (point_number / 10 - 1))][1];
            return mpoint;
        }
    }
    //水位显示1
    class WaterLevelView1 extends View{
        Paint paint;
        public WaterLevelView1(Context context){
            super(context);
            paint = new Paint();
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
        @Override
        protected void onDraw(Canvas canvas) {
            float left = 2400, top = 610, width = 100, height = 200, gap_w = 3, gap_h = 1;
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.WHITE);
            canvas.drawRect(left, top, left + width, top +height, paint);

            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            if(init_count == 4)
                paint.setColor(Color.RED);
            for(int i = init_count; i < 5; i++){
                if(i != 0){
                    canvas.drawRect(left + gap_w, top + (float)(height * i / 5.0) + gap_h,
                            left + width - gap_w - 1,  top + (float)(height * (i + 1) / 5.0) - gap_h, paint);
                }
                else
                    canvas.drawRect(left + gap_w, top + (float)(height * i / 5.0) + gap_h + 1,
                            left + width - gap_w - 1,  top + (float)(height * (i + 1) / 5.0) - gap_h, paint);
            }

            invalidate();
        }
    }

    class WaterLevelView2 extends View{
        Paint paint;
        public WaterLevelView2(Context context){
            super(context);
            paint = new Paint();
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
        @Override
        protected void onDraw(Canvas canvas) {
            float left = 2265, top = 610, width = 100, height = 200, gap_w = 3, gap_h = 1;
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5);
            paint.setColor(Color.WHITE);
            canvas.drawRect(left, top, left + width, top +height, paint);

            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            if(init_count1 == 4)
                paint.setColor(Color.RED);
            for(int i = init_count1; i < 5; i++){
                if(i != 0){
                    canvas.drawRect(left + gap_w, top + (float)(height * i / 5.0) + gap_h,
                            left + width - gap_w - 1,  top + (float)(height * (i + 1) / 5.0) - gap_h, paint);
                }
                else
                    canvas.drawRect(left + gap_w, top + (float)(height * i / 5.0) + gap_h + 1,
                            left + width - gap_w - 1,  top + (float)(height * (i + 1) / 5.0) - gap_h, paint);
            }

            invalidate();
        }
    }
    //障碍距离图形化显示
    class CustomView_obstacle extends View {
        private int pixel_screen[] = {2560, 1440};
        Paint paint;

        public CustomView_obstacle(Context context) {
            super(context);
            paint = new Paint(); //设置一个笔刷大小是3的黄色的画笔
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(3);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float oval_size[] = {750, 400};
            float oval_sizeL[] = {850, 500};
            float dSweep = 20;
            float sAngle[] = {360, 90, 180, 270};
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
            RectF oval_large = new RectF(pixel_screen[0] / 2  - oval_sizeL[0], pixel_screen[1] / 2  - oval_sizeL[1],
                    pixel_screen[0] / 2  + oval_sizeL[0], pixel_screen[1] / 2  + oval_sizeL[1]);
            canvas.drawOval(oval_large, paint);

            RectF oval_small = new RectF(pixel_screen[0] / 2  - oval_size[0], pixel_screen[1] / 2  - oval_size[1],
                    pixel_screen[0] / 2  + oval_size[0], pixel_screen[1] / 2  + oval_size[1]);
            canvas.drawOval(oval_small, paint);
            paint.setStyle(Paint.Style.FILL);
            for(int i = 0; i < 4; i++){
                int front_flag = 0;
                Path path = new Path();
                if(i == 3)
                    front_flag = 1;
                else
                    front_flag = i + 2;
                if(Double.parseDouble(text_array[front_flag]) < 6.0 && Double.parseDouble(text_array[front_flag]) >= -2.0)
                    paint.setColor(0x44ff0000);
                else
                    paint.setColor(0x4400ff00);
                float space[] = {24, 14};
                float tmp_oval_size[] = {oval_size[0], oval_size[1]};
                for(int j = 2; j >= 0; j--){
                    tmp_oval_size[0] =  oval_size[0] + (space[0] + space[1]) * j;
                    tmp_oval_size[1] =  oval_size[1] + (space[0] + space[1]) * j;
                    path.moveTo(pixel_screen[0] / 2 + (tmp_oval_size[0] * (float)Math.cos(deg2rad(sAngle[i]- dSweep))), pixel_screen[1] / 2 + tmp_oval_size[1] * (float)Math.sin(deg2rad(sAngle[i] - dSweep)));
                    path.lineTo(pixel_screen[0] / 2 + (tmp_oval_size[0] + space[0])* (float)Math.cos(deg2rad(sAngle[i]- dSweep)), pixel_screen[1] / 2 + (tmp_oval_size[1] + space[0]) * (float)Math.sin(deg2rad(sAngle[i] - dSweep)));
                    RectF oval = new RectF(pixel_screen[0] / 2  - tmp_oval_size[0], pixel_screen[1] / 2  - tmp_oval_size[1],
                            pixel_screen[0] / 2  + tmp_oval_size[0], pixel_screen[1] / 2  + tmp_oval_size[1]);
                    path.arcTo(oval, sAngle[i] - dSweep, dSweep * 2);
                    path.lineTo(pixel_screen[0] / 2 + (tmp_oval_size[0] + space[0]) * (float)Math.cos(deg2rad(sAngle[i] + dSweep)), pixel_screen[1] / 2 + (tmp_oval_size[1] + space[0]) * (float)Math.sin(deg2rad(sAngle[i] + dSweep)));
                    oval = new RectF(pixel_screen[0] / 2  - (tmp_oval_size[0] + space[0]), pixel_screen[1] / 2  - (tmp_oval_size[1] + space[0]),
                            pixel_screen[0] / 2  + tmp_oval_size[0] + space[0], pixel_screen[1] / 2  + (tmp_oval_size[1] + space[0]));
                    path.arcTo(oval, sAngle[i] + dSweep, -dSweep * 2);
                    canvas.drawPath(path, paint);
                }
            }

            invalidate();
        }
        private float deg2rad(float f){
            return (float)(f / 180.0 * Math.PI);
        }
    }

     //落点标定曲线图形化显示
    class CustomView1 extends View{
        Paint paint;
        public CustomView1(Context context) {
            super(context);
            paint = new Paint(); //设置一个笔刷大小是3的黄色的画笔
            paint.setColor(Color.RED);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(3);
        }
        //在这里我们将测试canvas提供的绘制图形方法
        @Override
        protected void onDraw(Canvas canvas) {
            Path path = new Path();
            paint.setStyle(Paint.Style.FILL);
            DensityUtil.point_calculate(point, nozzle_angle, yaw, pitch);
            //实现立体效果
            paint.setColor(Color.GRAY);
            paint.setStrokeWidth(8);
            for(int i = 0; i < point_number - 3; i++){
                canvas.drawCircle(point[i][0], point[i][1] + 8, 4, paint);
                if(i < point_number - 4){
                    canvas.drawLine(point[i][0], point[i][1] + 8, point[i + 1][0], point[i + 1][1] + 8, paint);
                }
            }

            paint.setStrokeWidth(9);
            paint.setColor(Color.RED);
            canvas.drawCircle(point[0][0], point[0][1] , 20, paint);
            for(int i = 0; i < point_number; i++){
                canvas.drawCircle(point[i][0], point[i][1], 5, paint);
                if(i < point_number - 1){
                    canvas.drawLine(point[i][0], point[i][1], point[i + 1][0], point[i + 1][1], paint);
                }
            }
            invalidate();
        }
    }

    //swap fpvWidget with mapWidget
    private void onViewClick(View view) {
        //放大fpvWidget
        if (view == fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);

            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;

        } else if (view == mapWidget && isMapMini) {//放大地图
            hidePanels();
            resizeFPVWidget(width, height, margin, 12);

            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
        }
    }
    //提示框

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        //重新设置fpv参数
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) primaryVideoView.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        primaryVideoView.setLayoutParams(fpvParams);

        parentView.removeView(primaryVideoView);
        parentView.addView(primaryVideoView, fpvInsertPosition);
    }
    //地图放大时隐藏相机

    //切换第一视角与第二个第一视角
    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
        }
    }

    private void updateSecondaryVideoVisibility() {
        if (secondaryFPVWidget.getVideoSource() == null) {
            secondaryVideoView.setVisibility(View.GONE);
        } else {
            secondaryVideoView.setVisibility(View.VISIBLE);
        }
    }

    private void hidePanels() {
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.HISTOGRAM_ENABLED), false, null);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED), false, null);
        }

        //These panels don't have a button state, so we can just hide them.
        findViewById(R.id.pre_flight_check_list).setVisibility(View.GONE);
        findViewById(R.id.rtk_panel).setVisibility(View.GONE);
        findViewById(R.id.speaker_panel).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
       //  Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mapWidget.onResume();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    private class ResizeAnimation extends Animation {

        private View mView;
        private int mToHeight;
        private int mFromHeight;

        private int mToWidth;
        private int mFromWidth;
        private int mMargin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }

    void initview(){
        voltage_battery_lowlevel = (TextView)findViewById(R.id.voltage_value_lowlevel);
        voltage_battery = (TextView)findViewById(R.id.voltage_value);
        textView_distance_front = (TextView)findViewById(R.id.text_view_distance_front);
        textView_distance_rear = (TextView)findViewById(R.id.text_view_distance_rear);
        textView_distance_left = (TextView)findViewById(R.id.text_view_distance_left);
        textView_distance_right = (TextView)findViewById(R.id.text_view_distance_right);
        textView_distance_up = (TextView)findViewById(R.id.text_view_distance_up);
        textView_distance_bottom = (TextView)findViewById(R.id.text_view_distance_bottom);
        texView_depth_of_water = (TextView)findViewById(R.id.distance_of_depth_water);
        parentView = (ViewGroup) findViewById(R.id.root_view);
        fpvWidget = findViewById(R.id.fpv_widget);
        mView = (Button)findViewById(R.id.button1);
        aSwitch = (Switch)findViewById(R.id.s_v);
        takeoffSwitch = (Switch)findViewById(R.id.takeoff_switch);
        primaryVideoView = (RelativeLayout) findViewById(R.id.fpv_container);
        secondaryVideoView = (FrameLayout) findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);
        automatic_infor = (TextView)findViewById(R.id.text_automatic);
    }

    //弹窗提示信息
    public void dialogShowBeforeTakeOff(){
//        Toast.makeText(CompleteWidgetActivity.this, "启动", Toast.LENGTH_SHORT).show();
        AlertDialog.Builder builder  = new AlertDialog.Builder(CompleteWidgetActivity.this);
        builder.setTitle("确认" ) ;
        builder.setMessage("确认水箱question1？" ) ;
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                AlertDialog.Builder builder  = new AlertDialog.Builder(CompleteWidgetActivity.this);
                builder.setTitle("确认" ) ;
                builder.setMessage("确认question2？" ) ;
                builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AlertDialog.Builder builder  = new AlertDialog.Builder(CompleteWidgetActivity.this);
                        builder.setTitle("确认" ) ;
                        builder.setMessage("是否确认？" ) ;
                        builder.setPositiveButton("是",null );
                        builder.setNegativeButton("否", null);
                        builder.show();
                    }
                });
                builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(CompleteWidgetActivity.this, "question2无法起飞", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });
        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(CompleteWidgetActivity.this, "question1无法起飞", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });
        builder.show();
    }
}

