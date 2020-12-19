package com.dji.ux.sample;

import android.content.Context;
// 获取像素值
public class DensityUtil {
    static int point_number = 20000;
    private static double g = 10000;
    //旋转轴的坐标
    private static double stick_rotation_axes_x = -100;
    private static double stick_rotation_axes_z = 120;
    private static double y_deviation_stick = 10;
    //喷杆长度
    private static double stick_length = 1000;
    //喷嘴水流速度
    private static double nozzle_velocity = 50000;
    //相机内参
    private static double focus[] = {800, 740};
    private static double deviation[] = {640, 360};
    private static double distortion[] = {-0.125, 0.096, 0.0012, -0.000123, -0.011135};

    private static double nozzle_angle_plus = 8 / 180.0 * Math.PI;
    //相机图片像素
    private static int pixel_camera[] = {1280, 720};
    private static int pixel_screen[] = {2560, 1440};

    public static int dip2px(Context context, float dpValue) {
        //获取屏幕大小
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
    //计算投影点
    public static void point_calculate(float [][] point, double nozzle_angle, double yaw, double pitch){
        //喷嘴位置
        double nozzle_x_coor;
        double nozzle_z_coor;
        nozzle_x_coor = stick_rotation_axes_x + stick_length * Math.cos(nozzle_angle);
        nozzle_z_coor = stick_rotation_axes_z - stick_length * Math.sin(nozzle_angle);
        double[] x1 = new double[point_number];
        double[] y1 = new double[point_number];
        double[] z1 = new double[point_number];
        double inter = nozzle_x_coor;
        for(int i = 0; i < point_number; i++){
            x1[i] = inter;
            inter += 1;
            y1[i] = y_deviation_stick;
            z1[i] =  - Math.tan(nozzle_angle + nozzle_angle_plus) * (x1[i] - nozzle_x_coor) +
                    0.5 * g * Math.pow(x1[i] - nozzle_x_coor, 2) /
                            Math.pow(nozzle_velocity * Math.cos(nozzle_angle + nozzle_angle_plus), 2)
                    + nozzle_z_coor;
        }

        double R_yaw_data[][] = {{Math.cos(yaw), Math.sin(yaw), 0}, {-Math.sin(yaw),  Math.cos(yaw), 0}, {0, 0, 1}};
        double R_pitch_data[][] = {{Math.cos(pitch), 0, -Math.sin(pitch)}, {0, 1, 0}, {Math.sin(pitch), 0, Math.cos(pitch)}};
        MyMatrix R_yaw = new MyMatrix(3, 3, R_yaw_data);
        MyMatrix R_pitch = new MyMatrix(3, 3, R_pitch_data);
        MyMatrix R_rotate = R_yaw.multiply(R_pitch);
        R_rotate = R_rotate.transpose();
        for(int i = 0; i < point_number; i++){
            double point_data[][] = {{x1[i]}, {y1[i]}, {z1[i]}};
            MyMatrix point_vector = new MyMatrix(3, 1, point_data);
            MyMatrix v_image = R_rotate.multiply(point_vector);
            double inter_x;
            double inter_y;

            inter_x = v_image.getMatrix(1, 0) / v_image.getMatrix(0,0);
            inter_y = v_image.getMatrix(2, 0) / v_image.getMatrix(0,0);
            //根据畸变进行投影
            double r = Math.sqrt(Math.pow(inter_x, 2) + Math.pow(inter_y, 2));
            inter_x = inter_x * (1 + distortion[0] * Math.pow(r, 2) + distortion[1] * Math.pow(r, 4) + distortion[4] * Math.pow(r, 6)) + 2 * distortion[2] * inter_x * inter_y + distortion[3] * (Math.pow(r, 2) + 2 * Math.pow(inter_x, 2));
            inter_y = inter_y * (1 + distortion[0] * Math.pow(r, 2) + distortion[1] * Math.pow(r, 4) + distortion[4] * Math.pow(r, 6)) + distortion[2] * (Math.pow(r, 2) + 2 * Math.pow(inter_y, 2)) + 2 * distortion[3] * inter_x * inter_y;

            inter_x = focus[0] * inter_x + deviation[0];
            inter_y = focus[1] * inter_y + deviation[1];

            point[i][0] = (float) (inter_x * pixel_screen[0] / pixel_camera[0]);
            point[i][1] = (float) (inter_y * pixel_screen[1] / pixel_camera[1]) + 80;
        }
    }


}
