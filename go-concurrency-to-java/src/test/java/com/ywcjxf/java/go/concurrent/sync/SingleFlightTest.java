package com.ywcjxf.java.go.concurrent.sync;

import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleFlightTest {

    @Test
    public void test(){

        SingleFlight.Group g = new SingleFlight.Group();
        BlockingQueue<String> c = new SynchronousQueue<>();
        AtomicInteger calls = new AtomicInteger(0);

        Callable<String> fn = ()->{
            calls.incrementAndGet();
            String s = c.take();
            return s;
        };

        //这里的回调接口实现嵌入到下面的g.DoChan里也行 就是每次运行时都new一个对象
        //这样可以 因为SingleFlight是用key区分的 实现里就没区分回调接口实现
        //只是调用

        final int n = 10;
        CountDownLatch wg = new CountDownLatch(10);

        for (int i = 0; i < n; i++) {
            new Thread(()->{
                //1.使用do
                //SingleFlight.Result r = g.Do("key",fn);

                try {
                    SingleFlight.Result r = g.DoChan("key",fn).get();
                    String str = (String)(r.Val);
                    if(!str.equals("bar")){
                        System.out.println("got "+str+"; want bar");
                    }
                }catch (ExecutionException ex){
                    System.out.println("Do error:"+ex.getCause());
                }

                wg.countDown();
            }).start();
        }

        try {
            Thread.sleep(100);
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }
        try {
            c.put("bar");
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }
        try {
            wg.await();
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }
        int got = calls.get();
        if(got!=1){
            System.out.println("number of calls = "+got+"; want 1");
        }
        System.out.println(got);
    }

    //上面的测试例子来自于
    // https://yangxikun.com/golang/2017/03/07/golang-singleflight.html
}
