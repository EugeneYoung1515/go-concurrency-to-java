package com.ywcjxf.java.go.concurrent.sync;

import org.junit.Test;

public class WaitGroupTest {

    @Test
    public void mainWaitSubThreadsFinishAndReuse(){
        WaitGroup waitGroup = new WaitGroup();

        for (int j = 0; j < 10; j++) {

            for (int i = 0; i < 3; i++) {
                waitGroup.add();
                new Thread(()->{

                    try {
                        Thread.sleep(2000);
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }

                    System.out.println("done");
                    waitGroup.done();

                }).start();
            }

            waitGroup.await();
            System.out.println("all");

        }
    }
}
