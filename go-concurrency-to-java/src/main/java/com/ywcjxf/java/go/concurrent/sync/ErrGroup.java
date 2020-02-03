package com.ywcjxf.java.go.concurrent.sync;

import com.ywcjxf.java.go.concurrent.context.Context;
import com.ywcjxf.java.go.concurrent.util.Go;
import com.ywcjxf.java.go.concurrent.util.RunnableThrowException;
import com.ywcjxf.java.go.concurrent.util.Util;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public class ErrGroup {
    public static class Group{
        private Runnable cancel;
        private WaitGroup wg = new WaitGroup();
        private Once once = new Once();
        private Exception err;

        public Group() {
        }

        private Group(Runnable cancel) {
            this.cancel = cancel;
        }

        /*
        public Exception Await(){
            wg.await();
            if(cancel!=null){
                cancel.run();
            }
            return err;
        }
        */

        public void await() throws ExecutionException{
            wg.await();
            if(cancel!=null){
                cancel.run();
            }
            if(err!=null){
                throw new ExecutionException(err);
            }
        }


        /*
        public void Go(RunnableThrowException f){
            Go(Util.throwExceptionToReturn(f));
        }

        public void Go(Supplier<Exception> f){
            wg.add();

            Go.go(()->{
                try {

                    Exception err = f.get();
                    if(err!=null){
                        once.Do(()->{
                            this.err = err;
                            if(cancel!=null){
                                cancel.run();
                            }
                        });
                    }

                }finally {
                    wg.done();
                }
            });
        }
        */

        public void go(RunnableThrowException f){
            wg.add();

            Go.go(()->{
                try {

                    Exception err = null;
                    try {
                        f.run();
                    }catch (Exception ex){
                        err =ex;
                    }
                    Exception tmp = err;

                    if(tmp!=null){
                        once.Do(()->{
                            this.err = tmp;
                            if(cancel!=null){
                                cancel.run();
                            }
                        });
                    }

                }finally {
                    wg.done();
                }
            });


        }

    }

    public static class WithContext{
        public Group group;
        public Context context;

        public WithContext(Group group, Context context) {
            this.group = group;
            this.context = context;
        }

    }

    public static WithContext withContext(Context context){
        Context.WithCancel withCancel = Context.withCancel(context);
        Context c = withCancel.ctx;
        return new WithContext(new Group(withCancel.cancelFunc),c);
    }
}
