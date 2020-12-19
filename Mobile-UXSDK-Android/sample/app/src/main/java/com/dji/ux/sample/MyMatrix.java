package com.dji.ux.sample;

public class MyMatrix {
    private int row;//行

    private int col;//列
    //private double value;
    double [][]Data;

    public MyMatrix(int row, int col,double [][]Data) {
        this.row = row;
        this.col = col;
        //    this.value = value;
        this.Data = Data;
    }

    public void setMatrix(int row , int col, double value) {
        this.Data[row - 1][col - 1] = value;
    }

    public double getMatrix(int row, int col) {
        return Data[row][col] ;
    }

    public int width() {
        return row;
    }

    public int height() {
        return col;
    }

    public MyMatrix add(MyMatrix b) {
        if(this.width() != b.width() && this.height() != b.height()) {
            return null;
        }

        double add[][] = new double[this.row][this.col];
        for(int i = 0;i<col;i++) {
            for(int j = 0;j<row;j++) {
                add[i][j] = this.Data[i][j] + b.Data[i][j];
            }
        }
        MyMatrix another = new MyMatrix(this.col,this.row,add);
        System.out.println("after add:");
        return another;
    }

    public MyMatrix multiply(MyMatrix b) {
        if(this.col != b.row) {
            return null;
        }
        double mul[][] = new double[this.row][b.col];
        double temp = 0;
        for(int i = 0;i<this.row;i++) {
            for(int k = 0;k<b.col;k++) {
                for(int j = 0;j<this.col;j++) {
                    temp += this.Data[i][j] * b.Data[j][k];
                }
                mul[i][k] = temp;
                temp = 0;
            }
        }
        MyMatrix another = new MyMatrix(this.row, b.col, mul);
        System.out.println("after multiply:");
        return another;
    }

    public MyMatrix transpose() {
        double tran[][] = new double[this.row][this.col];
        for(int i = 0;i<this.row;i++) {
            for(int j = 0;j<this.col;j++) {
                tran[j][i] = this.Data[i][j];
            }
        }
        MyMatrix another = new MyMatrix(this.col,this.row,tran);
        System.out.println("after transpose:");
        return another;
    }
}
