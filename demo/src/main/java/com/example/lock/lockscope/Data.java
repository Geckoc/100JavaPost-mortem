package com.example.lock.lockscope;

import lombok.Getter;

/**
 * @ClassName: Data
 * @Create: 2022-10-19 11:35
 */
public class Data {
    @Getter
    private static int counter = 0;
    /**
     * 解决线程安全问题
     */
    private static Object locker = new Object();

    public static void reset(){
        counter = 0;
    }
    public synchronized void wrong(){
        counter++;
    }

    public void right(){
        synchronized (locker){
            counter++;
        }
    }
}
