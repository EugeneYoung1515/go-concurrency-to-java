package com.ywcjxf.java.go.concurrent.sync.prepare;

import com.ywcjxf.java.go.concurrent.sync.Once;

public class Singleton {
    private static Singleton singleton;
    private static Once once = new Once();

    private static Runnable runnable = ()->{
        singleton = new Singleton();
    };

    private Singleton() {
    }

    /*
    public static Singleton getInstance(){
        once.Do(()->{
            singleton = new Singleton();
        });

        return singleton;
    }
    */

    public static Singleton getInstance(){
        once.Do(runnable);

        return singleton;
    }

}
