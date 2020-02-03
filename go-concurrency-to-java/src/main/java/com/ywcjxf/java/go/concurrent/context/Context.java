package com.ywcjxf.java.go.concurrent.context;

import com.ywcjxf.java.go.concurrent.chan.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.ywcjxf.java.go.concurrent.context.ContextImpl.*;
import static com.ywcjxf.java.go.concurrent.context.ContextImpl.TimeCtx.scheduledExecutorService;

public interface Context {

    Deadline Deadline();

    class Deadline{
        public long deadline;
        public boolean ok;

        public Deadline(long deadline, boolean ok) {
            this.deadline = deadline;
            this.ok = ok;
        }

        public Deadline() {
        }
    }

    ReceiveChannel done();

    Exception err();

    Object value(Object key);

    Exception Canceled = new Exception("context canceled");

    Exception DeadlineExceeded = new Exception("context deadline exceeded");

    static Context background(){
        return background;
    }

    static Context todo(){
        return todo;
    }

    interface CancelFunc extends Runnable{
        default void cancel(){
            run();
        }
    }

    static WithCancel withCancel(Context parent){
        ContextImpl.CancelCtx c = newCancelCtx(parent);
        propagateCancel(parent,c);
        return new WithCancel(c,()->c.cancel(true,Canceled));
    }

    class WithCancel{
        public ContextImpl.CancelCtx ctx;
        public CancelFunc cancelFunc;

        public WithCancel(ContextImpl.CancelCtx ctx, CancelFunc cancelFunc) {
            this.ctx = ctx;
            this.cancelFunc = cancelFunc;
        }

        public void cancel(){
            cancelFunc.cancel();
        }
    }

    static WithCancel withDeadline(Context parent,long d){
        Deadline deadline = parent.Deadline();
        long cur = deadline.deadline;
        if(deadline.ok && cur<d){//要不要等于
            return withCancel(parent);
        }
        //ContextImpl.TimeCtx c = new ContextImpl.TimeCtx(newCancelCtx(parent),d);
        ContextImpl.TimeCtx c = new ContextImpl.TimeCtx(parent,d);
        propagateCancel(parent,c);
        long dur = d-System.currentTimeMillis();
        if(dur<=0){
            c.cancel(true,DeadlineExceeded);
            return new WithCancel(c,()->c.cancel(false,Canceled));
        }
        c.mu.lock();
        try {
            if(c.err==null){
                //c.timer = Executors.newScheduledThreadPool(1);
                //c.future = c.timer.schedule(()->c.cancel(true,DeadlineExceeded),dur, TimeUnit.MILLISECONDS);

                c.timer = scheduledExecutorService.schedule(()->c.cancel(true,DeadlineExceeded),dur, TimeUnit.MILLISECONDS);
            }
            return new WithCancel(c,()->c.cancel(true,Canceled));
        }finally {
            c.mu.unlock();
        }
    }


    static WithCancel withTimeout(Context parent,long timeout){
        return withDeadline(parent,System.currentTimeMillis()+timeout);
    }

    static Context WithValue(Context parent,Object key,Object val){
        if(key==null){
            throw new NullPointerException("nil key");
        }

        return new ContextImpl.ValueCtx(parent,key,val);
    }
}
