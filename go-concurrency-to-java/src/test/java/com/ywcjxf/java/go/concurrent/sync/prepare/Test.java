package com.ywcjxf.java.go.concurrent.sync.prepare;

import com.ywcjxf.java.go.concurrent.sync.GoRwMutex;

import java.util.concurrent.ConcurrentHashMap;

public class Test {
    private ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>(16);
    private GoRwMutex goRwMutex = new GoRwMutex();

    //目的 这个有竞争的跑两次 之后在跑下面的reset
    public String get(String key,String value){

        goRwMutex.readLock();
        System.out.println("get");
        try {
            map.putIfAbsent(key, value);
            String v = map.get(key);

            return v;
        }finally {
            goRwMutex.readUnlock();
        }

    }

    public void reset(){

        goRwMutex.lock();
        System.out.println("reset");
        try {
            map = new ConcurrentHashMap<>(16);
        }finally {
            goRwMutex.unlock();
        }

    }
}
