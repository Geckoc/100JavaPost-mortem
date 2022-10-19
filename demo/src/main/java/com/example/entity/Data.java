package com.example.entity;

/**
 * @ClassName: Data
 * @Author: Richard_Chen
 * @Create: 2022-10-19 11:35
 */
public class Data {

    private static int counter = 0;

    public static int getCounter() {
        return counter;
    }

    public static void reset(){
        counter = 0;
    }
    public synchronized void wrong(){
        counter++;
    }
}
