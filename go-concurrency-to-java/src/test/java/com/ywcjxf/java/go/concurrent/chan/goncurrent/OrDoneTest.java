package com.ywcjxf.java.go.concurrent.chan.goncurrent;

import com.ywcjxf.java.go.concurrent.chan.impl.third.io.github.anolivetree.goncurrent.Chan;
import com.ywcjxf.java.go.concurrent.chan.impl.third.io.github.anolivetree.goncurrent.Select;
import org.junit.Test;

public class OrDoneTest {
    public static<T> Chan<T> orDone(Chan<T> done, Chan<T> c){
        Chan<T> valStream = Chan.create(0);

        new Thread(()->{

            try {
                while (true) {

                    Select select = new Select();
                    select.receive(done).receive(c);

                    int i = select.select();
                    //System.out.println("index:"+i);

                    if(i==0){
                        return;
                    }
                    if(i==1){
                        if(select.getData()==null){
                            //System.out.println("null");
                            return;
                        }

                        //System.out.println("d"+select.getData());
                        new Select().receive(done).send(valStream,select.getData()).select();
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
        Chan<Integer> chan = Chan.create(0);
        new Thread(()->{
            for (int i = 0; i < 10000; i++) {
                chan.send(i);
            }
            chan.close();
        }).start();

        Chan<Integer> cc = orDone(Chan.create(0),chan);
        while (true){
            Object v = cc.receive();
            if(v==null){
                System.out.println("break");
                break;
            }
            System.out.println(v);
        }
        System.out.println("done");
    }
}
