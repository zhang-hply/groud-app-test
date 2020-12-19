package com.dji.ux.sample.custom;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dji.ux.sample.CompleteWidgetActivity;
import com.dji.ux.sample.R;

import java.util.Timer;
import java.util.TimerTask;

import dji.ux.model.base.BaseDynamicWidgetAppearances;
import dji.ux.widget.BatteryWidget;
import dji.common.battery.ConnectionState;

/**
 * Override default battery widget with custom UI resources and logic
 */
public class CustomizedBatteryWidget extends BatteryWidget {

    private TextView batteryValue;
    private ImageView batteryIcon;
    private int batteryIconRes;
    private int batteryIconErrorRes;

    public CustomizedBatteryWidget(Context context) {
        this(context, null, 0);
    }

    public CustomizedBatteryWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomizedBatteryWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /** Inflate custom layout for this widget */
    @Override
    public void initView(Context context, AttributeSet attrs, int defStyle) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.customized_battery_widget, this);
        batteryValue = (TextView) view.findViewById(R.id.textview_battery_value);
        batteryIcon = (ImageView) view.findViewById(R.id.imageview_battery_icon);
        batteryValue.setText("0%");
        batteryIcon.setImageResource(R.mipmap.battery_error);
    }

    @Override
    protected BaseDynamicWidgetAppearances getWidgetAppearances() {
        return null;
    }

    /** Called when battery percentage changes */
    @Override
    public void onBatteryPercentageChange(int percentage) {
        batteryValue.setText(percentage + "%");

        if (percentage > 0 && percentage < 5) {
            batteryIconRes = R.mipmap.battery0;
        } else if (percentage >= 5 && percentage < 15) {
            batteryIconRes = R.mipmap.battery1;
        } else if (percentage >= 15 && percentage < 25) {
            batteryIconRes = R.mipmap.battery2;
        } else if (percentage >= 25 && percentage < 35) {
            batteryIconRes = R.mipmap.battery3;
        } else if (percentage >= 35 && percentage < 45) {
            batteryIconRes = R.mipmap.battery4;
        } else if (percentage >= 45 && percentage < 55) {
            batteryIconRes = R.mipmap.battery5;
        } else if (percentage >= 55 && percentage < 65) {
            batteryIconRes = R.mipmap.battery6;
        } else if (percentage >= 65 && percentage < 75) {
            batteryIconRes = R.mipmap.battery7;
        } else if (percentage >= 75 && percentage < 85) {
            batteryIconRes = R.mipmap.battery8;
        } else if (percentage >= 85 && percentage < 95) {
            batteryIconRes = R.mipmap.battery9;
        } else if (percentage >= 95 && percentage <= 100) {
            batteryIconRes = R.mipmap.battery10;
        }

        updateBatteryIcon();
    }

    /** Called when battery state changes from error to normal or vice versa */
    @Override
    public void onBatteryConnectionStateChange(ConnectionState status) {
        if (status != ConnectionState.NORMAL) {
            batteryIconErrorRes = R.mipmap.battery_error;
        } else {
            batteryIconErrorRes = 0;
        }
        updateBatteryIcon();
    }

    private void updateBatteryIcon() {
        if (batteryIconErrorRes != 0) {
            batteryIcon.setImageResource(batteryIconErrorRes);
        } else {
            batteryIcon.setImageResource(batteryIconRes);
        }
    }
}
//获取云台姿态
//        mAircraft = (Aircraft) DJISDKManager.getInstance().getProduct();
//        GimbalKey gimbalKey = GimbalKey.create(GimbalKey.ATTITUDE_IN_DEGREES);
//        GetCallback getCallback = new GetCallback() {
//            @Override
//            public void onSuccess(Object o) {
//                pitch = -(((Attitude) o).getPitch()) / 180.0 * Math.PI;
//                yaw = (((Attitude) o).getYaw()) / 180.0 * Math.PI;
//            }
//
//            @Override
//            public void onFailure(DJIError djiError) {
//
//            }
//        };
// KeyManager.getInstance().getValue(gimbalKey, getCallback);

//mbattery.getLatestWarningRecord(new CommonCallbacks.CompletionCallbackWith<WarningRecord>() {
//@Override
//public void onSuccess(WarningRecord warningRecord) {
//        if(warningRecord.isBatteryAbnormalConnection())
//        Toast.makeText(CompleteWidgetActivity.this, "电池连接不正常", Toast.LENGTH_LONG).show();
//        if(warningRecord.isCurrentOverloaded())
//        Toast.makeText(CompleteWidgetActivity.this, "电池电流过载", Toast.LENGTH_LONG).show();
//        if(warningRecord.isShortCircuited())
//        Toast.makeText(CompleteWidgetActivity.this, "电池短路", Toast.LENGTH_LONG).show();
//        if(warningRecord.getLowVoltageCellIndex() != -1)
//        Toast.makeText(CompleteWidgetActivity.this, warningRecord.getLowVoltageCellIndex() + "号电芯电压过低", Toast.LENGTH_LONG).show();
//        if(warningRecord.isOverHeated())
//        Toast.makeText(CompleteWidgetActivity.this, "电池过热", Toast.LENGTH_LONG).show();
//        if(warningRecord.isLowTemperature())
//        Toast.makeText(CompleteWidgetActivity.this, "电池温度过低", Toast.LENGTH_LONG).show();
//        if(warningRecord.getDamagedCellIndex() != -1)
//        Toast.makeText(CompleteWidgetActivity.this, warningRecord.getLowVoltageCellIndex() + "号电芯已损坏", Toast.LENGTH_LONG).show();
//        }
//@Override
//public void onFailure(DJIError djiError) {
//
//        }
//        });

//发送数据到onboard
//        button.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//
////                data[0] = 2;
////                data[1] = 3;
////                String s = new String(data);
////               // Toast.makeText(CompleteWidgetActivity.this, "no product connected", Toast.LENGTH_SHORT).show();
////                if(mAircraft != null && mAircraft.isConnected()){
////                    Toast.makeText(CompleteWidgetActivity.this, "Data: " + s, Toast.LENGTH_SHORT).show();
////                    mFlightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
////                        @Override
////                        public void onResult(DJIError error) {
////
////                        }
////                    });
////                }
////                else
////                    Toast.makeText(CompleteWidgetActivity.this, "no product connected", Toast.LENGTH_SHORT).show();
//            }
//        });



//画框
//                for(int i = 0; i < 10; i++){
//                    width_1[i] = 0; height_1[i] = 0; x_coor[i] = 0; y_coor[i] = 0; degrees[i] = 0;
//                }
//                String[] text_array = msg.split(",");
//                for(int i = 0; i < 5; i++){
//                    if (text_array[i].equals("80.0"))
//                        text_array[i] = "SAFE";
//                    else
//                        text_array[i] = text_array[i] + "m";
//                }
//                textView_distance_front.setText(text_array[0]);
//                textView_distance_rear.setText(text_array[1]);
//                textView_distance_left.setText(text_array[2]);
//                textView_distance_right.setText(text_array[3]);
//                whether_to_wash.setText(text_array[4]);
//
//                l = (text_array.length - 5) / 5;
//                for(int i = 0; i < l; i++){
//                    x_coor[i] = Integer.parseInt(text_array[5 * i + 5]) * 1920 / 1280;
//                    y_coor[i] = Integer.parseInt(text_array[5 * i + 6]) * 1080 / 720 + 50;
//                    height_1[i] = Integer.parseInt(text_array[5 * i + 7]) * 3 / 2 / 2;
//                    width_1[i] = Integer.parseInt(text_array[5 * i + 8]) * 3 / 2 / 2;
//                    degrees[i] = Integer.parseInt(text_array[5 * i + 9]);
//                }

//            mbattery.getLevel1CellVoltageBehavior(new CommonCallbacks.CompletionCallbackWith<LowVoltageBehavior>(){
//                @Override
//                public void onSuccess(LowVoltageBehavior lowVoltageBehavior) {
////                    if(lowVoltageBehavior.equals(LowVoltageBehavior.FLASH_LED))
//                //    int voltage_value = lowVoltageBehavior.value();
//                    Log.d("LowVoltageBehavior",  "FLASH_LED");
//                }
//
//                @Override
//                public void onFailure(DJIError djiError) {
//
//                }
//            } );
//

//    private TextView textView_remained_time;
//    private TextView textView_angle_of_nozzle;
//    private int[] width_1 = new int[10];
//    private int[] height_1 = new int[10];
//    private int[] degrees = new int[10];
//    private int[] x_coor = new int[10];
//    private int[] y_coor = new int[10];
//    private int l = 0;

//    Timer mtimer = new Timer();
//    Handler mhandler = new Handler(){
//        public void handleMessage(Message msg){
//            super.handleMessage(msg);
//            if(msg.what == 100){
//                addContentView(new CompleteWidgetActivity.CustomView1(CompleteWidgetActivity.this), new ViewGroup.LayoutParams(2560, 1600));
//                Log.d("service_water_curve", "Handle::" + "Pitch: " + pitch * 180.0 / Math.PI + ". Yaw: " +
//                        yaw * 180.0 / Math.PI + "nozzle_angle:" + nozzle_angle * 180.0 / Math.PI+
//                        ", u:" + point[0][0] + ", v:" + point[0][1]);
//            }
//        }
//    };
//
//    TimerTask task = new TimerTask() {
//        @Override
//        public void run() {
//            Message message = new Message();
//            message.what = 100;
//            mhandler.sendMessage(message);
//        }
//    };