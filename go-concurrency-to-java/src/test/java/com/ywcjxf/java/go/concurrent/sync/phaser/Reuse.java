package com.ywcjxf.java.go.concurrent.sync.phaser;

import java.util.concurrent.Phaser;

public class Reuse {
    public static void main(String[] args) {//重用
        Phaser phaser = new Phaser(1);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                phaser.register();

                int m = j+1;
                new Thread(()->{

                    try {
                        Thread.sleep(m*1000);
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                    System.out.println(m);
                    phaser.arriveAndDeregister();
                }).start();
            }

            phaser.arriveAndAwaitAdvance();
            System.out.println("done");
        }
    }
}
