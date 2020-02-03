package com.ywcjxf.java.go.concurrent.chan.goncurrent;

import com.ywcjxf.java.go.concurrent.chan.impl.third.io.github.anolivetree.goncurrent.Chan;

public class Main {
    public static void main(String[] args) {
        Chan<Integer> chan = Chan.create(2);
        new Thread(()->{

            try {
                Thread.sleep(3000);
            }catch (InterruptedException ex){
                ex.printStackTrace();
            }

            while (true){
                Integer i = chan.receive();
                if(i==null){
                    break;
                }
                System.out.println(i);
            }

        }).start();

        chan.send(1);
        chan.send(2);
        chan.close();
    }
}
