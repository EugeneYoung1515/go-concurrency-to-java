package com.ywcjxf.java.go.concurrent.chan.impl;

import com.ywcjxf.java.go.concurrent.chan.AbstractChannel;
import com.ywcjxf.java.go.concurrent.chan.ChannelAlreadyClosedException;
import com.ywcjxf.java.go.concurrent.chan.impl.third.pychan.GenericsChan;

public class PyChanChannel<T> extends AbstractChannel<T>{
    private GenericsChan<T> chan;

    public PyChanChannel() {
        this(0);
    }

    public PyChanChannel(int i) {
        super(i);
        chan = new GenericsChan<>(i);
    }

    @Override
    public Object getNativeChannel() {
        return chan;
    }

    @Override
    public void send(T t) throws ChannelAlreadyClosedException {
        super.send(t);
        try {
            chan.put(t);
        }catch (Exception ex){
            throw new ChannelAlreadyClosedException(ex);
        }

    }


    @Override
    public T receive() {
        return chan.get();
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
