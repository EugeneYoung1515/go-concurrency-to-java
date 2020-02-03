package com.ywcjxf.java.go.concurrent.sync;

import com.ywcjxf.java.go.concurrent.sync.prepare.Singleton;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;

public class OnceTest {

    @Test
    public void runOnce(){
        Once once = new Once();
        Runnable runnable = ()->{System.out.println("test");};

        for (int i = 0; i < 100; i++) {
            new Thread(()->{

                once.Do(runnable);

            }).start();
        }
    }

    @Test
    public void singleton(){
        for (int i = 0; i < 1000; i++) {
            System.out.println(Singleton.getInstance());
        }
    }

    @Test
    public void concurrentHashMapPutOnceAndReturn(){
        //实现concurrenthashmap的只设置一次并返回值

        Once once = new Once();
        ConcurrentHashMap<String,String> concurrentHashMap = new ConcurrentHashMap<>(16);

        once.Do(()->{
            concurrentHashMap.put("q","qwe");
        });

        once.Do(()->{
            concurrentHashMap.put("q","qwer");
        });

        System.out.println(concurrentHashMap.get("q"));
    }
}
