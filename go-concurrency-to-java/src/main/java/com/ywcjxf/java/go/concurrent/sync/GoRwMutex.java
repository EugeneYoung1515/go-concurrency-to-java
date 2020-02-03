package com.ywcjxf.java.go.concurrent.sync;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class GoRwMutex {
    private ReentrantLock mutex = new ReentrantLock();
    private Semaphore readerSem = new Semaphore(0);
    private Semaphore writerSem = new Semaphore(0);
    private AtomicInteger readerCount = new AtomicInteger(0);
    private AtomicInteger readerWait = new AtomicInteger(0);

    private int rwmutexMaxReaders = 1<<30;

    //private Semaphore semaphore = new Semaphore(rwmutexMaxReaders);//这部分golang的源码没有 是自己添加的 为了像锁一样 把变量刷回主内存

    //考虑 happen before

    public void readLock(){
        /*
        try {
            semaphore.acquire();//这部分golang的源码没有 是自己添加的 为了像锁一样 把变量刷回主内存
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }
        */

        if(readerCount.incrementAndGet()<0){
            try {
                readerSem.acquire();
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }
    }

    public void readUnlock(){
        if(readerCount.decrementAndGet()<0){
            if(readerWait.decrementAndGet()==0){
                writerSem.release();
            }
        }

        //semaphore.release();
    }

    public void lock(){

        mutex.lock();
        int r = readerCount.addAndGet(-rwmutexMaxReaders)+rwmutexMaxReaders;
        if(r!=0&&readerWait.addAndGet(r)!=0){
            try {
                writerSem.acquire();
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }
        }
    }

    public void unlock(){
        int r = readerCount.addAndGet(rwmutexMaxReaders);
        for(int i=0;i<r;i++){
            readerSem.release();
        }
        mutex.unlock();
    }

}
