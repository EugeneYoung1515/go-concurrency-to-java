package com.ywcjxf.java.go.concurrent.sync;

import com.ywcjxf.java.go.concurrent.context.Context;
import org.junit.Test;

import java.util.concurrent.ExecutionException;

public class ErrGroupTest {
    @Test
    public void test(){
        ErrGroup.Group group = new ErrGroup.Group();
        for (int i = 0; i < 3; i++) {
            int m =i;
            group.go(()->{

                if(m==2){
                   throw new RuntimeException("except");
                }else{
                    System.out.println(m+" done");
                }
            });
        }

        group.go(()->{
            try {
                Thread.sleep(4000);
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }

            System.out.println(" done");
        });


        try {
            group.await();
        }catch (ExecutionException ex){
            ex.printStackTrace();
        }
    }

    @Test
    public void withContextTest(){
        ErrGroup.WithContext withContext = ErrGroup.withContext(Context.background());
        ErrGroup.Group group = withContext.group;

        for (int i = 0; i < 3; i++) {
            int m =i;
            group.go(()->{
                try {
                    Thread.sleep(4000);
                }catch (InterruptedException ex){
                    ex.printStackTrace();
                }

                if(m==2){
                    throw new RuntimeException("except");
                }else{
                    System.out.println(m+" done");
                }
            });
        }

        group.go(()->{
            withContext.context.done().receive();
            System.out.println(" done");
        });


        try {
            group.await();
        }catch (ExecutionException ex){
            ex.printStackTrace();
        }

        System.out.println(withContext.context.done().receive());
    }
}
