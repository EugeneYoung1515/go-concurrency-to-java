package com.ywcjxf.java.go.concurrent.chan;

import com.ywcjxf.java.go.concurrent.chan.Channel;
import com.ywcjxf.java.go.concurrent.chan.ReceiveChannel;
import com.ywcjxf.java.go.concurrent.chan.Select;
import com.ywcjxf.java.go.concurrent.chan.impl.*;
import org.junit.Test;

import java.util.concurrent.CyclicBarrier;

public class OrDoneTest {
    public static<T> ReceiveChannel<T> orDone(ReceiveChannel<T> done, ReceiveChannel<T> c){
        CyclicBarrier cyclicBarrier = new CyclicBarrier(4);

        //Channel<T> valStream = new PyChanChannel<>(0);
                //Channel<T> valStream = new GonCurrentChannel<>(0);

        for (int i = 0; i < 4; i++) {
            new Thread(()->{

                try {
                    while (true) {

                        //Select.SelectResult<T> selectResult = new PyChanSelect<T>().register(done).register(c).selectAfterRegister();
                                //Select.SelectResult<T> selectResult = new GonCurrentSelect<T>().register(done).register(c).selectAfterRegister();

                        if(selectResult.index==0){
                            return;
                        }
                        if(selectResult.index==1){
                            if(selectResult.data==null){
                                System.out.println("null");
                                return;
                            }

                            //Select.SelectResult<T> selectResult1 = new PyChanSelect<T>().register(done).register(valStream,selectResult.data).selectAfterRegister();
                                    //Select.SelectResult<T> selectResult1 = new GonCurrentSelect<T>().register(done).register(valStream,selectResult.data).selectAfterRegister();
                        }
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    try {
                        if(cyclicBarrier.await()==0){
                            valStream.close();
                        }
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                }

            }).start();
        }
        return valStream;
    }

    @Test
    public void test() {
        CyclicBarrier cyclicBarrier = new CyclicBarrier(1);

        //Channel<Integer> chan = new PyChanChannel<>(0);
                //Channel<Integer> chan = new GonCurrentChannel<>(0);

        for (int j = 0; j < 1; j++) {
            new Thread(()->{
                for (int i = 0; i < 10000; i++) {
                    chan.send(i);
                }
                try {
                    if(cyclicBarrier.await()==0){
                        chan.close();
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }).start();
        }


        //ReceiveChannel<Integer> cc = orDone(new PyChanChannel<>(0),chan);
                //ReceiveChannel<Integer> cc = orDone(new GonCurrentChannel<>(0),chan);

        long start = System.currentTimeMillis();
        while (true){
            Object v = cc.receive();
            if(v==null){
                //return;
                break;
            }
            System.out.println(v);
        }

        System.out.println(System.currentTimeMillis()-start);
    }
}
