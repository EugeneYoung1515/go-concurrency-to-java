package com.ywcjxf.java.go.concurrent.sync;

import com.ywcjxf.java.go.concurrent.sync.prepare.Test2;
import com.ywcjxf.java.go.concurrent.sync.prepare.Test3;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class GoRwMutexTest {

    @Test
    public void useGoRwMutex(){
        com.ywcjxf.java.go.concurrent.sync.prepare.Test test = new com.ywcjxf.java.go.concurrent.sync.prepare.Test();
        CountDownLatch countDownLatch = new CountDownLatch(17);

        long start = System.currentTimeMillis();

        for(int i=0;i<16;i++){
            new Thread(()->{
                for(int j=0;j<100;j++){
                    if(test.get("test","qwe")==null){
                        System.out.println("null");
                    }
                }

                countDownLatch.countDown();

            }).start();
        }

        new Thread(()->{

            for(int j=0;j<100;j++){
                test.reset();
            }

            countDownLatch.countDown();

        }).start();

        try{
            countDownLatch.await();
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }

        System.out.println(System.currentTimeMillis()-start);
    }

    @Test
    public void useJavaReentrantWriteReadLock(){
        Test2 test = new Test2();
        CountDownLatch countDownLatch = new CountDownLatch(17);

        long start = System.currentTimeMillis();

        for(int i=0;i<16;i++){
            new Thread(()->{
                for(int j=0;j<100;j++){
                    if(test.get("test","qwe")==null){
                        System.out.println("null");
                    }
                }

                countDownLatch.countDown();

            }).start();
        }

        new Thread(()->{

            for(int j=0;j<100;j++){
                test.reset();
            }

            countDownLatch.countDown();

        }).start();

        try{
            countDownLatch.await();
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }

        System.out.println(System.currentTimeMillis()-start);

    }

    @Test
    public void useJavaStampedLock(){
        Test3 test = new Test3();
        CountDownLatch countDownLatch = new CountDownLatch(17);


        long start = System.currentTimeMillis();

        for(int i=0;i<16;i++){
            new Thread(()->{
                for(int j=0;j<100;j++){
                    if(test.get("test","qwe")==null){
                        System.out.println("null");
                    }
                }

                countDownLatch.countDown();

            }).start();
        }

        new Thread(()->{

            for(int j=0;j<100;j++){
                test.reset();
            }

            countDownLatch.countDown();

        }).start();

        try{
            countDownLatch.await();
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }

        System.out.println(System.currentTimeMillis()-start);

    }
}
