package com.ywcjxf.java.go.concurrent.chan.pychan;

import com.ywcjxf.java.go.concurrent.chan.impl.third.pychan.Chan;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class OrDoneTest {
    public static Chan orDone(Chan done,Chan c){
        Chan valStream = new Chan(0);

        new Thread(()->{

                try {
                    while (true) {
                        Chan.Holder holder = Chan.chanSelect(Arrays.asList(done, c), new ArrayList<>());
                        if (holder.chan == done) {
                            return;
                        }
                        if (holder.chan == c) {
                            Object message = holder.value;
                            if (message == null) {
                                System.out.println("null");
                                return;
                            }

                            //System.out.println("h:"+message);

                            Chan.Holder holder1 = Chan.chanSelect(Arrays.asList(done), Arrays.asList(new Chan.Holder(valStream, message)));
                        }
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }finally {
                    valStream.close();
                }

        }).start();

        return valStream;
    }

    @Test
    public void test() {
        Chan chan = new Chan(0);
        new Thread(()->{
            for (int i = 0; i < 10000; i++) {
                chan.put(i);
            }
            chan.close();
        }).start();

        Chan cc = orDone(new Chan(0),chan);
        while (true){
            Object v = cc.get();
            if(v==null){
                return;
            }
            System.out.println(v);
        }
    }
}
