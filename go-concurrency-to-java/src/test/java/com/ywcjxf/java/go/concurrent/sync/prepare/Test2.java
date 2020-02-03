package com.ywcjxf.java.go.concurrent.sync.prepare;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Test2 {
    private ConcurrentHashMap<String,String> map = new ConcurrentHashMap<>(16);
    private ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    private ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
    private ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();

    //目的 这个有竞争的跑两次 之后在跑下面的reset
    public String get(String key,String value){

        readLock.lock();
        System.out.println("get");
        try {
            map.putIfAbsent(key, value);
            String v = map.get(key);

            return v;
        }finally {
            readLock.unlock();
        }

    }

    public void reset(){

        writeLock.lock();
        System.out.println("reset");
        try {
            map = new ConcurrentHashMap<>(16);
        }finally {
            writeLock.unlock();
        }

    }
}
