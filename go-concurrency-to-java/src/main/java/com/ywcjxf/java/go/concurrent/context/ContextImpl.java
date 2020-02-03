package com.ywcjxf.java.go.concurrent.context;

import com.ywcjxf.java.go.concurrent.chan.*;
import com.ywcjxf.java.go.concurrent.util.Go;
import com.ywcjxf.java.go.concurrent.util.WatchDog;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;

 class ContextImpl {

     static class EmptyCtx implements Context{

        @Override
        public Deadline Deadline() {
            return new Deadline();
        }

        @Override
        public ReceiveChannel done() {
            //return new DefaultChannel(0);//有问题 要不要全部用null 替代nil channel
            return null;
        }

        @Override
        public Exception err() {
            return null;
        }

        @Override
        public Object value(Object key) {
            return null;
        }

        @Override
        public String toString(){//可能有问题
            if(this==background){
                return "context.Background";
            }
            if(this==todo){
                return "context.TODO";
            }
            return "unknown empty Context";//这一句是不是就一直不会被调用
        }

    }

    //静态变量
    static EmptyCtx background = new EmptyCtx();
    static EmptyCtx todo = new EmptyCtx();

    static CancelCtx newCancelCtx(Context parent){
        return new CancelCtx(parent);
    }

    static void propagateCancel(Context parent,Canceler child){
        ReceiveChannel done = parent.done();
        if(done==null){
            return;
        }

        Select.SelectResult s = new DefaultSelect().register(done).trySelectAfterRegister();
        if(s!=null&&s.index==0){
            child.cancel(false,parent.err());
            return;
        }
        CancelCtxWithBool cancelCtxWithBool = parentCancelCtx(parent);
        CancelCtx p = cancelCtxWithBool.cancelCtx;
        if(cancelCtxWithBool.bool){
            p.mu.lock();
            if(p.err!=null){
                child.cancel(false,p.err);
            }else{
                if(p.children==null){
                    p.children = new HashSet<>(16);
                }
                p.children.add(child);
            }
            p.mu.unlock();
        }else{

            //这里什么时候调用

            //给自定义 CancelCtx 实现 用
            System.out.println("go");
            Go.go(()->{

                Select.SelectResult ss = new DefaultSelect().register(parent.done()).register(child.done()).selectAfterRegister();
                if(ss.index==0){
                    child.cancel(false,parent.err());
                }

            });
        }

    }

     //静态变量
    //static Integer cancelCtxKey = 0;
     static Object cancelCtxKey = new Object();

    private static CancelCtxWithBool parentCancelCtx(Context parent){
        ReceiveChannel done = parent.done();
        if(done==closedchan||done==null){
            return new CancelCtxWithBool(null,false);
        }
        CancelCtx p = (CancelCtx) parent.value(cancelCtxKey);//这里会找爹 找爹的爹...
        if(p==null){
            return new CancelCtxWithBool(null,false);
        }
        p.mu.lock();
        boolean ok = p.done == done;
        p.mu.unlock();
        if(!ok){
            return new CancelCtxWithBool(null,false);
        }
        return new CancelCtxWithBool(p,true);
    }

    static class CancelCtxWithBool{
        public CancelCtx cancelCtx;
        public boolean bool;

        public CancelCtxWithBool(CancelCtx cancelCtx, boolean bool) {
            this.cancelCtx = cancelCtx;
            this.bool = bool;
        }
    }

    static void removeChild(Context parent,Canceler child){
        CancelCtxWithBool cancelCtxWithBool = parentCancelCtx(parent);
        CancelCtx p = cancelCtxWithBool.cancelCtx;
        if(!cancelCtxWithBool.bool){
            return;
        }
        p.mu.lock();
        if(p.children!=null){
            p.children.remove(child);
        }
        p.mu.unlock();
    }

    interface Canceler{
        void cancel(boolean removeFromParent,Exception error);
        ReceiveChannel done();
    }

     //静态变量
    static Channel closedchan = new DefaultChannel(0);

     //静态语句块
    static {
        closedchan.close();
    }

    static String contextName(Context c){

        try {
            c.getClass().getDeclaredMethod("toString");//这里可能有问题
            //这一句的判断是不是多余的
            //判断自定义 CancelCtx 实现

            return c.toString();

            //上面的作用是 有重写toString方法 就调用toString方法

        }catch (NoSuchMethodException ex){
            return c.getClass().getSimpleName();//这一句是不是一直不会被调用
        }

        //要是不给自定义实现用 只留下面一句就行了
        //return c.toString();
    }

    static class CancelCtx implements Context, Canceler{
        public Context context;

        ReentrantLock mu = new ReentrantLock();
        Channel done;
        Set<Canceler> children;
        Exception err;

        public CancelCtx(Context context) {
            this.context = context;
        }

        @Override
        public Object value(Object key){
            //if(key==(Integer)cancelCtxKey){
            if(key==cancelCtxKey){
                return this;
            }
            return this.context.value(key);
        }

        @Override
        public ReceiveChannel done() {
            mu.lock();
            if(done==null){
                done = new DefaultChannel(0);
            }
            Channel d = done;
            mu.unlock();
            return d;
        }

        @Override
        public Exception err() {
            mu.lock();
            Exception err = this.err;
            mu.unlock();
            return err;
        }



        @Override
        public String toString(){
               return contextName(context)+".WithCancel";
        }

        @Override
        public void cancel(boolean removeFromParent, Exception err) {
            //System.out.println("cancel 0");

            if(err==null){
                throw new RuntimeException("context: internal error: missing cancel error");
            }

            //System.out.println("cancel 01");

            mu.lock();
            //System.out.println("getlock");
            if(this.err != null){
                mu.unlock();
                return;
            }

            //System.out.println("cancel 02");

            this.err = err;
            if(done == null){
                done = closedchan;
            }else{
                done.close();
                //System.out.println(System.currentTimeMillis()+"close channel");
            }

            //System.out.println("cancel 03");

            //try {
                if(children!=null){

                    for(Canceler child:children){
                        //try {
                            child.cancel(false,err);
                        //}catch (Exception ex){
                            //ex.printStackTrace();
                            //throw ex;
                        //}

                    }
                }
            //}catch (Exception ex){
                //ex.printStackTrace();
                //throw ex;
            //}

            //System.out.println("cancel 04");
            children = null;
            mu.unlock();

            //System.out.println("cancel 05");

            if(removeFromParent){
                removeChild(context,this);
            }

        }

        @Override
        public Deadline Deadline() {//这里可能有问题
            //System.out.println("deadline null");
            //return null;
            return context.Deadline();
        }

    }

    static class TimeCtx extends CancelCtx implements Context, Canceler{
        //CancelCtx cancelCtx;

        /*
        ScheduledExecutorService timer;
        Future future;
        */

        static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
        //static SimpleScheduledExecutor scheduledExecutorService = new SimpleScheduledExecutor();
        Future timer;

        long deadline;


        static {
            System.out.println("watch");
            new WatchDog((ThreadPoolExecutor)scheduledExecutorService).start();
            /*
            new Thread(()->{

                for (int i = 0; i < 3; i++) {
                    try {
                        Thread.sleep(5000);
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }

            }).start();
            */
        }


        //public TimeCtx(CancelCtx cancelCtx, long deadline) {
        public TimeCtx(Context context, long deadline) {
            super(context);

            //this.cancelCtx = cancelCtx;
            this.deadline = deadline;

        }

        @Override
        public Deadline Deadline(){
            return new Deadline(deadline,true);
        }

        @Override
        public String toString(){
            return contextName(/*cancelCtx.*/context)+".WithDeadline("+
                    deadline+" ["+(System.currentTimeMillis()-deadline)+"])";
        }

        @Override
        public void cancel(boolean removeFromParent, Exception err) {
            //System.out.println("cancel"+System.currentTimeMillis());

            //System.out.println("cancel 1");

            //cancelCtx.cancel(false,err);
            super.cancel(false,err);
            //System.out.println("cancel 2");
            if(removeFromParent){
                removeChild(/*cancelCtx.*/context,this);
            }
            //System.out.println("cancel 3");
            mu.lock();
            //System.out.println("cancel 4");
            /*
            if(timer!=null){
                future.cancel(true);
                timer.shutdownNow();
                timer = null;
            }
            */

            if(timer!=null){
                timer.cancel(true);
                timer = null;
            }
            mu.unlock();
        }

    }

    static String stringify(Object v){//这里可能有问题

        /*
        try {
            v.getClass().getDeclaredMethod("toString");
            return v.toString();

            //上面的作用是 有重写toString方法 就调用toString方法
        }catch (NoSuchMethodException ex){


            //在java里 是字符串就会走上面的调用
            //if(v instanceof String){
                //return (String)v;
            //}

            return "<not Stringer>";
        }
        */

        return v.toString();
    }

    static class ValueCtx implements Context,Canceler{
        public Context context;
        public Object key;
        public Object value;

        public ValueCtx(Context context, Object key, Object value) {
            this.context = context;
            this.key = key;
            this.value = value;
        }

        @Override
        public Deadline Deadline() {
            return context.Deadline();
        }

        @Override
        public ReceiveChannel done() {
            return context.done();
        }

        @Override
        public Exception err() {
            return context.err();
        }

        @Override
        public String toString(){
            /*
            return contextName(context)+".WithValue(type "+key.getClass().getSimpleName()+
                    ", val "+stringify(value)+")";
                    */

            //自己多加了一个打印key
            return contextName(context)+".WithValue(type "+key.getClass().getSimpleName()+", key "+stringify(key)+
                    ", val "+stringify(value)+")";

        }

        @Override
        public Object value(Object key) {


            if(this.key==key||(this.key!=null&&this.key.equals(key))){
                return value;
            }

            /*
            要不要像hashmap一样
                                if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;

             */

            return context.value(key);
        }

        @Override
        public void cancel(boolean removeFromParent, Exception error) {
            if(context instanceof Canceler){
                ((Canceler)context).cancel(removeFromParent,error);
            }else{
                throw new UnsupportedOperationException();
            }
        }
    }
}
//现在问题
//1。那个静态语句块里放 watchdog 或者 new 一个线程都行
//2。那个静态语句块放在TimeCtx下 或者 ContextImpl下 都行
//3。使用定时任务线程池 或者 new 一个线程 都行
//都会使得 deadline时间到了 和定时任务开始运行的时间 间隔很短
//去掉那个静态语句块 deadline时间到了 和定时任务开始运行的时间 间隔会边长很多

