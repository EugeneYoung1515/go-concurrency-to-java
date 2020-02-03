package com.ywcjxf.java.go.concurrent.contex;

import com.ywcjxf.java.go.concurrent.context.Context;
import org.junit.Test;

public class Main3 {

    @Test
    public void test(){
        Context context = Context.background();
        System.out.println(context);

        Context.WithCancel withCancel = Context.withCancel(context);
        Context context1 = withCancel.ctx;
        System.out.println(context1);

        Context.WithCancel withCancel1 = Context.withTimeout(context,2000);
        Context context2 = withCancel1.ctx;
        System.out.println(context2);

        Context valuectx = Context.WithValue(context,"q","z");
        System.out.println(valuectx);

        Context valuectx1 = Context.WithValue(context1,"q","qwe");
        System.out.println(valuectx1);

        Context.WithCancel cancel = Context.withCancel(valuectx);
        Context context3 = cancel.ctx;
        System.out.println(context3);
    }
}
