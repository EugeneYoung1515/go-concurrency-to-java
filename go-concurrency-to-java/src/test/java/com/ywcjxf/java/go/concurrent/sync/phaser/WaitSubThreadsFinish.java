package com.ywcjxf.java.go.concurrent.sync.phaser;

import java.util.concurrent.Phaser;

public class WaitSubThreadsFinish {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(3);
        phaser.register();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 3; i++) {
            new Thread(()->{

                try {
                    Thread.sleep(3000);
                }catch (InterruptedException ex){
                    ex.printStackTrace();
                }

                phaser.arrive();
            }).start();
        }

        phaser.arriveAndAwaitAdvance();//等待子线程完成
        System.out.println("done");
        System.out.println(System.currentTimeMillis()-start);
    }
}
