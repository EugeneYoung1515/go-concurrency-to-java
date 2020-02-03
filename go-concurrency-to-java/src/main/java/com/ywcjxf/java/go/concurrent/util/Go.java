package com.ywcjxf.java.go.concurrent.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Go {
    private static ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);

    public static void go(Runnable runnable/*,String... tag*/){
        /*
        Thread thread = new Thread(()->{
            runnable.run();
            if(tag.length==1) {
                System.out.println(tag[0]);
            }
        });
        if(tag.length==1) {
            thread.setName(tag[0]);
        }
        thread.start();
        */

        executorService.execute(runnable);
    }

    public static <T> void go(Consumer<T> consumer,T parameter/*,String... tag*/){
        /*
        Thread thread = new Thread(()->{
            consumer.accept(parameter);
            if(tag.length==1) {
                System.out.println(tag[0]);
            }
        });
        if(tag.length==1) {
            thread.setName(tag[0]);
        }
        thread.start();
        */

        executorService.execute(()->{
            consumer.accept(parameter);
        });
    }

    public static <A,B> void go(BiConsumer<A,B> consumer, A parameter1,B parameter2){
        executorService.execute(()->{
            consumer.accept(parameter1,parameter2);
        });
    }
    }
