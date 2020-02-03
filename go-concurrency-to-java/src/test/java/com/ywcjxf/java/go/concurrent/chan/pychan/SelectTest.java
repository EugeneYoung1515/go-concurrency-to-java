package com.ywcjxf.java.go.concurrent.chan.pychan;

import com.ywcjxf.java.go.concurrent.chan.impl.third.pychan.Chan;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SelectTest {
    static void selectWithTwoChan() throws InterruptedException {
        //selectWithNChan(2, 50, 50, 2, 0, 300, false);
        //selectWithNChan(2, 50, 50, 0, 2, 300, false);

        //selectWithNChan(2, 50, 50, 1, 1, 300, false);

        //selectWithNChan(2, 1, 1, 1, 1, 300, false);

        //selectWithNChan(2, 50, 50, 2, 0, 300, true);
        //selectWithNChan(2, 50, 50, 0, 2, 300, true);
        //selectWithNChan(2, 50, 50, 1, 1, 300, true);
    }

    @Test
    public void test() throws InterruptedException{
        long start = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            selectWithTwoChan();

            System.out.println("fi "+i);
        }
        System.out.println(System.currentTimeMillis()-start);
    }


    static void selectWithNChan(int chanCnt, int threadPerChan, int testPerThread, int sendCnt,
                                int receiveCnt, int interval,
                                boolean hasDefault) throws InterruptedException {

        assert chanCnt == sendCnt + receiveCnt;
        int selectCnt = chanCnt * threadPerChan * testPerThread;

        ExecutorService executorService = Executors.newFixedThreadPool(chanCnt * threadPerChan);

        Chan[] chans =  new Chan[chanCnt];
        for (int j = 0; j < chans.length; j++) {
            chans[j] = new Chan(0);
        }

        CountDownLatch latch = new CountDownLatch(selectCnt);

        List<Integer> receiveFromSelector = Collections.synchronizedList(new ArrayList<>());
        List<Integer> receiveFromSenderChan = Collections.synchronizedList(new ArrayList<>());
        List<Integer> receiveFromSelectorDefault = Collections.synchronizedList(new ArrayList<>());

        int tmpSendCnt = sendCnt;
        int tmpReceiveCnt = receiveCnt;

        for (int i = 0; i < chanCnt; i++) {
            int finalI = i;
            Chan chan = chans[i];

            final boolean isSend = --tmpSendCnt >= 0 || --tmpReceiveCnt < 0;
            for (int j = 0; j < threadPerChan; j++) {
                executorService.execute(() -> {
                    try {

                        for (int k = 0; k < testPerThread; k++) {
                            if (isSend)
                                chan.put(finalI * threadPerChan * testPerThread + k);
                            else
                                receiveFromSelector.add((Integer)chan.get());
                            latch.countDown();
                            if (interval > 0) {
                                try {
                                    TimeUnit.MICROSECONDS.sleep(interval);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    }catch (Exception ex){
                        ex.printStackTrace();
                    }
                });
            }
        }

        executorService.shutdown();


        try {

            for (int i = 0; i < selectCnt; i++) {

                List<Chan> rList = new ArrayList<>(10);
                for (int j = 0; j < sendCnt; j++) {
                    rList.add(chans[j]);
                }
                List<Chan.Holder> sList = new ArrayList<>(10);
                for (int j = 0; j < receiveCnt; j++) {
                    sList.add(new Chan.Holder(chans[sendCnt+j],i));
                }

                /*
                SelectAction<Integer> select;
                if(hasDefault){
                    select = Selector.trySelect(list);
                }else{
                    select = Selector.select(list);
                }
                */

                Chan.Holder holder;
                if(hasDefault){
                    holder = Chan.chanSelect(rList,sList);
                }else{
                    holder = Chan.tryChanSelect(rList,sList);
                }

                if(holder!=null){
                    if(rList.indexOf(holder.chan)>-1){
                        if(holder.value==null){
                            System.out.println("shit");
                        }
                        receiveFromSelectorDefault.add((Integer)(holder.value));
                        //System.out.println(holder.value);
                    }
                }else{
                    //System.out.println("de");
                    receiveFromSelectorDefault.add(i);
                    latch.countDown();
                }

                /*
                if(select!=null){
                    if(select.index()<sendCnt){
                        //System.out.println(select.index());
                        if(select.message()==null){
                            System.out.println("shit");
                        }
                        receiveFromSenderChan.add(select.message());
                    }
                }else{
                    receiveFromSelectorDefault.add(i);
                    latch.countDown();
                }
                */


            }


        }catch (Exception e){
            e.printStackTrace();
        }

        latch.await();

        assertEquals(selectCnt,
                receiveFromSelector.size() + receiveFromSenderChan.size() + receiveFromSelectorDefault.size());
        assertTrue(receiveFromSelector.stream().allMatch(data -> data != null && data < selectCnt));
        if (!receiveFromSenderChan.stream().allMatch(data -> data != null && data < selectCnt)){
            throw new RuntimeException("fail");
        }
    }

    public static void assertEquals(int i,int j){
        if(i!=j){
            throw new RuntimeException("not equal");
        }
    }

    public static void assertTrue(boolean t){
        if(!t){
            throw new RuntimeException("not true");
        }
    }

}
