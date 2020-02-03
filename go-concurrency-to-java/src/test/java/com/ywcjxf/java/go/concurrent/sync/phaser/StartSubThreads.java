package com.ywcjxf.java.go.concurrent.sync.phaser;

import java.util.concurrent.Phaser;

public class StartSubThreads {
    public static void main(String[] args) {
        Phaser phaser = new Phaser(1);
        for (int i = 0; i < 10; i++) {
            phaser.register();
            new Thread() {
                public void run() {
                    phaser.arriveAndAwaitAdvance(); // await all creation
                    System.out.println("test");
                }
            }.start();
        }

        try {
            Thread.sleep(3000);
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }
        System.out.println("qwe");
        phaser.arriveAndDeregister();//启动子线程
    }
}
