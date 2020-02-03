package com.ywcjxf.java.go.concurrent.contex;

import com.ywcjxf.java.go.concurrent.context.Context;
import org.junit.Test;

public class Main2 {

    @Test
    public void test() {
        Context.WithCancel withCancel = Context.withCancel(Context.WithValue(Context.background(),"q","qaz"));
        Context ctx = withCancel.ctx;

        //toString测试
        //System.out.println(ctx);

        for (int i = 0; i < 4; i++) {
            int m =i+1;
            new Thread(()->{

                Context.WithCancel c = Context.withTimeout(ctx,m*1000);
                Context cctx = c.ctx;

                //toString测试
                //System.out.println((m-1)+" "+cctx);

                for (int j = 0; j < 4; j++) {
                    int n = j;
                    new Thread(()->{
                        Context.WithCancel cc = Context.withTimeout(cctx,2000);
                        Context ccctx = cc.ctx;

                        //toString测试
                        //System.out.println((m-1)+" "+n+" "+ccctx);

                        //测试内部类的名字
                        //System.out.println(ccctx.getClass().getSimpleName());

                        //取值测试
                        //System.out.println((m-1)+" "+n+ccctx.value("q"));

                        ccctx.done().receive();
                        System.out.println((m-1)+" "+n+" done");
                    }).start();
                }

                System.out.println("继续");

                cctx.done().receive();
                System.out.println((m-1)+"done");

            }).start();
        }

        try {
            Thread.sleep(10000);
        }catch (InterruptedException ex){
            ex.printStackTrace();
        }

        withCancel.cancel();
    }
}
