package com.ywcjxf.java.go.concurrent.contex;

import com.ywcjxf.java.go.concurrent.context.Context;
import org.junit.Test;

public class Main {

    @Test
    public void test() {
        /*
        try {
            Thread.sleep(10000);
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }
        */

        long start = System.currentTimeMillis();
        //Context.WithCancel withCancel = Context.withTimeout(Context.Background(),10000);
        Context.WithCancel withCancel = Context.withDeadline(Context.background(),System.currentTimeMillis()+5000);

        Context ctx = withCancel.ctx;

        System.out.println(ctx.err());
        //withCancel.cancel();

        System.out.println(ctx.done().receive());

        long t;
        System.out.println("现在是:"+(t=System.currentTimeMillis()));
        System.out.println("经过了: "+(t-start));

        System.out.println("deadline: "+ctx.Deadline().deadline);
        System.out.println(System.currentTimeMillis()-t);

        System.out.println(ctx);
        System.out.println(System.currentTimeMillis()-t);

        System.out.println(ctx.err());
    }

}
