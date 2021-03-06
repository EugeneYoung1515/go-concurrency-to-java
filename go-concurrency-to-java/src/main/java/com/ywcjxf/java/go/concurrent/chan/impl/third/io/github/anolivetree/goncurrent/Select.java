/**
 * Copyright (C) 2015 Hiroshi Sakurai
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ywcjxf.java.go.concurrent.chan.impl.third.io.github.anolivetree.goncurrent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Select {

    private ArrayList<Chan> mChan = new ArrayList<Chan>();
    private ArrayList<Object> mSendData = new ArrayList<Object>();

    private ArrayList<Shuffle> shuffles = new ArrayList<>(10);

    private int i = 0;

    private static class Shuffle{
        private Chan mChan;
        private Object mSendData;
        private int i;

        public Shuffle(Chan mChan, Object mSendData) {
            this.mChan = mChan;
            this.mSendData = mSendData;
        }

        public Shuffle(Chan mChan, Object mSendData, int i) {
            this.mChan = mChan;
            this.mSendData = mSendData;
            this.i = i;
        }
    }

    private Object mData;
    private final Random rand = new Random();

    /**
     * Add a channel to receive from.
     * @param chan
     * @return
     */
    public Select receive(Chan chan) {
        shuffles.add(new Shuffle(chan,ThreadContext.sReceiveFlag,i));
        i++;

        //mChan.add(chan);
        //mSendData.add(ThreadContext.sReceiveFlag);

        return this;
    }

    /**
     * Add a channel to send to and a data. Passing closed channel will cause an Exception on select().
     * @param chan
     * @param data
     * @return
     */
    public Select send(Chan chan, Object data) {
        shuffles.add(new Shuffle(chan,data,i));
        i++;

        //mChan.add(chan);
        //mSendData.add(data);

        return this;
    }

    /**
     *
     * @return index of the channel read or written. -1 when interrupted.
     */
    public int select() {
        return selectInternal(false);
    }

    /**
     *
     * @return index of the channel read or written. -1 when no channel is ready.
     */
    public int selectNonblock() {
        return selectInternal(true);
    }

    public int getIndex(Shuffle shuffle){
        /*
        for (int j = 0; j < shuffles.size(); j++) {
            Shuffle s = shuffles.get(j);
            if(s.i==shuffle.i){
                return j;
            }
        }
        return -1;
        */

        //return shuffles.indexOf(shuffle);

        return shuffle.i;
    }

    private int selectInternal(boolean nonblock) {
        List<Shuffle> afterShuffles = new ArrayList<>(shuffles);
        //System.out.println(shuffles);
        Collections.shuffle(afterShuffles);
        //System.out.println(afterShuffles);
        for (Shuffle s:afterShuffles){
            mChan.add(s.mChan);
            mSendData.add(s.mSendData);
        }

        mData = null;
        Chan.sLock.lock();

        ThreadContext context = null;
        try {
            context = ThreadContext.get();
            if (Config.DEBUG_PRINT) {
                System.out.println("call select context=" + context);
            }
            if (Config.DEBUG_CHECK_STATE) {
                context.ensureHasNoChan();
            }


            context.setChanAndData(mChan, mSendData);
            if (Config.DEBUG_PRINT) {
                System.out.println(" nchan = " + mChan.size());
            }
            mChan = new ArrayList<Chan>();
            mSendData = new ArrayList<Object>();

            while (true) {

                // find a channel which is ready to send or receive
                int index = findAvailableChanRandomAndProcess(context);
                if (index >= 0) {
                    if (Config.DEBUG_PRINT) {
                        System.out.printf("select: found available chan. i=%d\n", index);
                    }
                    //return index;
                    //return shuffles.indexOf(afterShuffles.get(index));
                    return getIndex(afterShuffles.get(index));
                }

                if (nonblock) {
                    return -1;
                }

                // no channel is available. Add self to waiting list of all channels.
                addToAllChan(context);

                // wait
                try {
                    if (Config.DEBUG_PRINT) {
                        System.out.printf("select: waiting\n");
                    }
                    context.mCond.await();
                    if (Config.DEBUG_PRINT) {
                        System.out.printf("select: woken up\n");
                    }
                } catch (InterruptedException e) {
                    if (Config.DEBUG_PRINT) {
                        System.out.printf("select: interrupted\n");
                    }
                    context.removeFromAllChannel();
                    return -1;
                }

                // woken up by someone. might be close()

                if (context.mUnblockedChanIndex == -1) {
                    // wokenup by close()
                    if (Config.DEBUG_PRINT) {
                        System.out.printf("select: woken up by close(). remove context from all channel\n");
                    }
                    context.removeFromAllChannel();
                    continue;
                }

                // no need to call context.removeFromAllChannle() because it is called by a peer
                if (context.mChan.size() > 0) {
                    throw new RuntimeException("chan exist.");
                }

                mData = context.mReceivedData;
                int mTargetIndex = context.mUnblockedChanIndex;
                if (mTargetIndex == -1) {
                    throw new RuntimeException("illegal state");
                }
                //return mTargetIndex;
                //return shuffles.indexOf(afterShuffles.get(mTargetIndex));
                return getIndex(afterShuffles.get(mTargetIndex));
            }

        } finally {
            context.clearChan();
            Chan.sLock.unlock();
        }
    }


    private int findAvailableChanRandomAndProcess(ThreadContext context) {
        int numChan = context.mChan.size();
        for (int n = 0, i = rand.nextInt(numChan); n < numChan; n++) {
            Chan ch = context.mChan.get(i);
            if (ch != null) {
                Object data = context.mSendData.get(i);
                if (data == ThreadContext.sReceiveFlag) {
                    Object peek = ch.receive(true);
                    if (peek != null) {
                        mData = ((Chan.Result)peek).data;
                        return i;
                    }
                } else {
                    boolean dontBlock = ch.send(data, true);
                    if (dontBlock) {
                        mData = null;
                        return i;
                    }
                }
            }

            i++;
            if (i >= numChan) {
                i = 0;
            }
        }
        return -1;
    }

    private void addToAllChan(ThreadContext context) {
        if (Config.DEBUG_PRINT) {
            System.out.println("select: adding context to all channels");
        }
        int numChan = context.mChan.size();
        for (int i = 0; i < numChan; i++) {
            Chan ch = context.mChan.get(i);
            if (Config.DEBUG_PRINT) {
                System.out.println(" ch=" + ch);
            }
            if (ch == null) {
                continue;
            }
            Object data = context.mSendData.get(i);
            if (data == ThreadContext.sReceiveFlag) {
                if (Config.DEBUG_PRINT) {
                    System.out.printf("  added to receiverList\n");
                }
                ch.addToReceiverList(context);
            } else {
                if (Config.DEBUG_PRINT) {
                    System.out.printf("  added to senderList\n");
                }
                ch.addToSenderList(context);
            }
        }
    }

    /**
     * Get received data. Call this after select() returns.
     * @return
     */
    public Object getData() {
        return mData;
    }

}
