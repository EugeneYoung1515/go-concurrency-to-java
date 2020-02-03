package com.ywcjxf.java.go.concurrent.sync;

import java.util.concurrent.Phaser;

public class WaitGroup {
    private Phaser phaser = new Phaser(1);

    public void add(int i){
        phaser.bulkRegister(i);
    }

    public void add(){
        add(1);
    }

    public void done(){
        phaser.arriveAndDeregister();
    }

    public void await(){
        phaser.arriveAndAwaitAdvance();
    }

}
