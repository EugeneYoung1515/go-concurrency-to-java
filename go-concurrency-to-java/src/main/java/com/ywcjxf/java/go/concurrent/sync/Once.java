package com.ywcjxf.java.go.concurrent.sync;

import java.util.concurrent.atomic.AtomicInteger;

public class Once {
    private AtomicInteger done = new AtomicInteger(0);
    //private volatile int done2 = 0;

    public void Do(Runnable runnable){
        if(done.get()==1){
            return;
        }
        synchronized (this){
            if(done.get()==0){
                runnable.run();
            }
            done.set(1);
        }
    }

    /*
    public void Do2(Runnable runnable){
        if(done.get()==0) {
            synchronized (this) {
                if (done.get() == 0) {
                    runnable.run();
                }
                done.set(1);
            }
        }
    }

    public void Do3(Runnable runnable){
        if(done2==0) {
            synchronized (this) {
                if (done2 == 0) {
                    runnable.run();
                }
                done2=1;
            }
        }
    }
    */

}
