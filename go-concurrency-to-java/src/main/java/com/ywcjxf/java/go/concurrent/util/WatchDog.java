package com.ywcjxf.java.go.concurrent.util;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WatchDog {
    private ThreadPoolExecutor threadPoolExecutor;

    public WatchDog(ThreadPoolExecutor threadPoolExecutor) {
        this.threadPoolExecutor = threadPoolExecutor;
    }

    public void start(){
        Thread t = new Thread(()->{

            int times = 0;

            try {
                Thread.sleep(5000);
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }

            while (true){

                int count = 0;

                for (int i = 0; i < 3; i++) {
                    if(!threadPoolExecutor.getQueue().isEmpty()){
                        break;
                    }else{
                        if(threadPoolExecutor.getActiveCount()==0){
                            count++;
                        }else{
                            break;
                        }
                    }
                }

                if(count==3){
                    times++;
                }else{
                    times=0;
                }

                System.out.println("times:"+times);

                if(times==3){
                    threadPoolExecutor.shutdown();
                    try {
                        threadPoolExecutor.awaitTermination(5, TimeUnit.SECONDS);
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                    threadPoolExecutor.shutdownNow();
                    break;
                }

                System.out.println("watchdog run");

                try {
                    Thread.sleep(5000);
                }catch (InterruptedException ex){
                    ex.printStackTrace();
                }
            }

        });

        t.setDaemon(true);
        t.start();
    }
}
