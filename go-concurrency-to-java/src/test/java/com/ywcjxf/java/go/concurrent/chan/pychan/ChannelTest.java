package com.ywcjxf.java.go.concurrent.chan.pychan;

import com.ywcjxf.java.go.concurrent.chan.impl.third.pychan.Chan;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

public class ChannelTest {

    @Test
    public  void test() {
        CountDownLatch countDownLatch = new CountDownLatch(6);
        CountDownLatch countDownLatch1 = new CountDownLatch(3);

        Chan chan = new Chan(0);
        for (int j = 0; j < 3; j++) {
            new Thread(()->{
                for (int i = 0; i < 2000; i++) {
                    chan.put(i);
                }

                countDownLatch.countDown();

                countDownLatch1.countDown();
            }).start();
        }

        new Thread(()->{
            try {
                countDownLatch1.await();
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }

            chan.close();
        }).start();


        for (int i = 0; i < 3; i++) {
            new Thread(()->{
                for (int j = 0; j < 2001; j++) {
                    System.out.println(chan.get());
                }

                countDownLatch.countDown();
            }).start();
        }

        try {
            countDownLatch.await();
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }

        System.out.println("done");
    }
}
