package com.ywcjxf.java.go.concurrent.sync.prepare;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.StampedLock;

public class Test3 {
    private ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>(16);
    private StampedLock stampedLock = new StampedLock();
    //目的 这个有竞争的跑两次 之后在跑下面的reset
    public String get(String key,String value){

        stampedLock.asReadLock().lock();
        System.out.println("get");
        try {
            map.putIfAbsent(key, value);
            String v = map.get(key);

            return v;
        }finally {
            stampedLock.asReadLock().unlock();
        }

    }

    public void reset(){

        stampedLock.asWriteLock().lock();
        System.out.println("reset");
        try {
            map = new ConcurrentHashMap<>(16);
        }finally {
            stampedLock.asWriteLock().unlock();
        }

    }
}
