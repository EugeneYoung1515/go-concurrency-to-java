package com.ywcjxf.java.go.concurrent.util;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class Util {
    public static void runNoReturn(RunnableThrowException runnable){
        try {
            runnable.run();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static <V> V run(Callable<V> callable){
        try {
            return callable.call();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public static Supplier<Exception> throwExceptionToReturn(RunnableThrowException runnable){
        return ()->{
            try {
                runnable.run();
                return null;
            }catch (Exception ex){
                return ex;
            }
        };
    }

}
