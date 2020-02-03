package com.ywcjxf.java.go.concurrent.chan.impl;

import com.ywcjxf.java.go.concurrent.chan.AbstractChannel;
import com.ywcjxf.java.go.concurrent.chan.ChannelAlreadyClosedException;
import com.ywcjxf.java.go.concurrent.chan.impl.third.io.github.anolivetree.goncurrent.Chan;

public class GonCurrentChannel<T> extends AbstractChannel<T>{
    private Chan<T> chan;

    public GonCurrentChannel() {
        this(0);
    }

    public GonCurrentChannel(int i) {
        super(i);
        chan = Chan.create(i);
    }

    @Override
    public Object getNativeChannel() {
        return chan;
    }

    @Override
    public void send(T t) throws ChannelAlreadyClosedException {
        super.send(t);
        try {
            chan.send(t);
        }catch (Exception ex){
            throw new ChannelAlreadyClosedException(ex);
        }

    }


    @Override
    public T receive() {
        return chan.receive();
    }


    @Override
    public void close() throws ChannelAlreadyClosedException {
        try {
            chan.close();
        }catch (Exception ex){
            throw new ChannelAlreadyClosedException(ex);
        }

    }

}
