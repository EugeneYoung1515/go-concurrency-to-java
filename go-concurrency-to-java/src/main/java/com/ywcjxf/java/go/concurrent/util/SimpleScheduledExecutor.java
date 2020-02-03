package com.ywcjxf.java.go.concurrent.util;

import java.util.concurrent.*;

public class SimpleScheduledExecutor{

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        Thread thread = new Thread(()->{
            try {
                Thread.sleep(unit.toMillis(delay));
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }

            command.run();

        });

        thread.start();

        return new ScheduledFuture<Object>() {
            @Override
            public long getDelay(TimeUnit unit) {
                return 0;
            }

            @Override
            public int compareTo(Delayed o) {
                return 0;
            }

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Object get() throws InterruptedException, ExecutionException {
                return null;
            }

            @Override
            public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return null;
            }
        };
    }

}
