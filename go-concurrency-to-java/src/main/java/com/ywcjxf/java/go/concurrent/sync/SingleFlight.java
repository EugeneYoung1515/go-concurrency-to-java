package com.ywcjxf.java.go.concurrent.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

//能不能异常 也上泛型

public class SingleFlight {
    private static class Call<T>{
        private CountDownLatch wg;
        private T val;
        private Exception err;

        boolean forgotten;

        int dups;
        private List<BlockingQueue<Result<T>>> chans = new ArrayList<>(10);
        private List<Chan<T>> chans2 = new ArrayList<>(10);
    }

    public static class Group<T>{
        private ReentrantLock mu = new ReentrantLock();
        private Map<String,Call<T>> m;

        private static ExecutorService go = Executors.newFixedThreadPool(4);

        public Result<T> Do(String key, Callable<T> fn) throws ExecutionException{
                mu.lock();
                if(m==null){
                    m = new HashMap<>(16);
                }
                Call<T> c = m.get(key);
                if(c!=null){
                    c.dups++;
                    mu.unlock();
                    try {
                        //System.out.println("await");
                        c.wg.await();
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                    //System.out.println("return");
                    //return new Result<>(c.val,c.err,c.dups>0);

                    if(c.err!=null){
                        throw new ExecutionException(c.err);
                    }
                    return new Result<>(c.val,c.err,c.dups>0);
                }

                c = new Call<>();
                c.wg = new CountDownLatch(1);
                m.put(key,c);
                mu.unlock();

                doCall(c,key,fn);
                //return new Result<>(c.val,c.err,c.dups>0);

            if(c.err!=null){
                throw new ExecutionException(c.err);
            }
            return new Result<>(c.val,c.err,c.dups>0);

        }

        /*
        public BlockingQueue<Result<T>> DoChan(String key, Callable<T> fn){
            BlockingQueue<Result<T>> ch = new ArrayBlockingQueue<>(1);
            mu.lock();
            if(m==null){
                m = new HashMap<>(16);
            }

            Call<T> c = m.get(key);
            if(c!=null){
                c.dups++;
                c.chans.add(ch);
                mu.unlock();
                return ch;
            }
            c = new Call<>();
            c.chans.add(ch);
            c.wg = new CountDownLatch(1);
            m.put(key,c);
            mu.unlock();

            Call<T> cc = c;
            go.execute(()->{doCall(cc,key,fn);});

            return ch;
        }
        */

        public Chan DoChan(String key, Callable<T> fn){
            Chan<T> ch = new Chan<>();
            mu.lock();
            if(m==null){
                m = new HashMap<>(16);
            }

            Call<T> c = m.get(key);
            if(c!=null){
                c.dups++;
                c.chans2.add(ch);
                mu.unlock();
                return ch;
            }
            c = new Call<>();
            c.chans2.add(ch);
            c.wg = new CountDownLatch(1);
            m.put(key,c);
            mu.unlock();

            Call<T> cc = c;
            go.execute(()->{doCall(cc,key,fn);});

            return ch;
        }


        private void doCall(Call<T> c,String key,Callable<T> fn){
            try {
                c.val = fn.call();
            }catch (Exception ex){
                c.err = ex;
            }
            c.wg.countDown();

            mu.lock();
            if(!c.forgotten){
                m.remove(key);
            }
            for(BlockingQueue<Result<T>> ch:c.chans){
                try {
                    ch.put(new Result<>(c.val, c.err, c.dups > 0));
                }catch (InterruptedException ex){
                    ex.printStackTrace();
                }
            }

            for(Chan<T> ch:c.chans2){
                ch.set(new Result<>(c.val, c.err, c.dups > 0));
            }

            mu.unlock();
        }

        public void Forget(String key){
            mu.lock();
            Call<T> c = m.get(key);
            if(c!=null){
                c.forgotten = true;
            }
            m.remove(key);
            mu.unlock();
        }
    }

    public static class Result<T>{
        public T Val;
        public Exception Err;
        public boolean Shared;

        public Result(T val, Exception err, boolean shared) {
            Val = val;
            Err = err;
            Shared = shared;
        }
    }

    public static class Chan<T>{
        private CountDownLatch countDownLatch = new CountDownLatch(1);
        private Result<T> result;

        private void set(Result<T> result){
            this.result = result;
            countDownLatch.countDown();
        }

        public Result<T> get() throws ExecutionException{
            try {
                countDownLatch.await();
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }
            //return result;

            if(result.Err!=null){
                throw new ExecutionException(result.Err);
            }else{
                return result;
            }
        }
    }

}
